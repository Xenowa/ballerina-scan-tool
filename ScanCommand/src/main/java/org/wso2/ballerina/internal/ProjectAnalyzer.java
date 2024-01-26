package org.wso2.ballerina.internal;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticPropertyKind;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.internal.utilities.ScanToolConstants;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ProjectAnalyzer {
    public ArrayList<Issue> analyzeProject(Path userPath) {
        // Array to hold all issues
        ArrayList<Issue> issues = new ArrayList<>();

        // Get access to the project API
        Project project = ProjectLoader.loadProject(userPath);

        // For single file inputs with bal scan
        if (!userPath.toFile().isDirectory()) {
            // if the user provided file path belongs to a build project stop the analysis
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                return null;
            }

            Module tempModule = project.currentPackage().getDefaultModule();
            DocumentId documentId = project.documentId(userPath);
            ArrayList<Issue> detectedIssues = analyzeDocument(project, tempModule, documentId);
            issues.addAll(detectedIssues);
        } else {
            // Iterate through each module of the project
            project.currentPackage().moduleIds().forEach(moduleId -> {
                // Get access to the project modules
                Module module = project.currentPackage().module(moduleId);

                // Iterate through each ballerina test file in a ballerina project and perform static analysis
                module.testDocumentIds().forEach(testDocumentID -> {
                    ArrayList<Issue> detectedIssues = analyzeDocument(project, module, testDocumentID);
                    issues.addAll(detectedIssues);
                });

                // Iterate through each document of the Main module/project + submodules
                module.documentIds().forEach(documentId -> {
                    ArrayList<Issue> detectedIssues = analyzeDocument(project, module, documentId);
                    issues.addAll(detectedIssues);
                });
            });
        }

        // return the detected issues
        return issues;
    }

    public ArrayList<Issue> analyzeDocument(Project currentProject, Module currentModule, DocumentId documentId) {
        // Retrieve each document from the module
        Document document = currentModule.document(documentId);

        // Map to store the parsed & Compiled outputs
        Map<String, Object> compiledOutputs = new HashMap<>();

        // Retrieve the syntax tree from the parsed ballerina document
        compiledOutputs.put("syntaxTree", document.syntaxTree());

        // Retrieve the compilation of the module
        ModuleCompilation compilation = currentModule.getCompilation();

        // Retrieve the semantic model from the ballerina document compilation
        compiledOutputs.put("semanticModel", compilation.getSemanticModel());

        // Perform static code analysis
        // Array to hold analysis issues for each document
        ArrayList<Issue> internalIssues = new ArrayList<>();

        // Retrieve the current document path
        Path documentPath = currentProject.documentPath(documentId).orElse(null);
        if (documentPath != null) {
            // Set up the issue reporter here so issues can be reported from the static code analyzers
            Reporter issueReporter = new Reporter(internalIssues,
                    documentPath.toAbsolutePath().toString());


            // Perform internal static code analysis
            runInternalScans((SyntaxTree) compiledOutputs.get("syntaxTree"),
                    (SemanticModel) compiledOutputs.get("semanticModel"),
                    issueReporter);

            // External rules compiler plugins analysis
            // ========
            // METHOD 1 (using compiler plugins with diagnostics) [Currently Implemented]
            // ========
            // - Runs when a package compilation is performed through project API
            runCustomScans(currentModule, currentProject, issueReporter);
        }

        // Return the analyzed file results
        return internalIssues;
    }

    // For rules that can be implemented using the syntax tree model
    public void runInternalScans(SyntaxTree syntaxTree, SemanticModel semanticModel, Reporter issueReporter) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize(issueReporter);
    }

    public void runCustomScans(Module currentModule, Project currentProject, Reporter issueReporter) {
        // Step 1: Check if the module is the default module
        if (currentModule.isDefaultModule()) {
            // Step 2: Get compilation of the whole package once
            PackageCompilation engagedPlugins = currentProject.currentPackage().getCompilation();

            // Step 3: Create an issues array to hold external issues
            ArrayList<Issue> externalIssues = new ArrayList<>();


            // Step 4: Iterate through diagnostics and retrieve external issues (Should not be performed with the
            // diagnostics API)
            engagedPlugins.diagnosticResult().diagnostics().forEach(diagnostic -> {
                String issueType = diagnostic.diagnosticInfo().code();

                if (issueType.equals(ScanToolConstants.CUSTOM_CHECK_VIOLATION)) {
                    List<DiagnosticProperty<?>> properties = diagnostic.properties();

                    // Retrieve the Issue property and add it to the issues array
                    AtomicReference<String> externalIssueRuleID = new AtomicReference<>(null);
                    AtomicReference<String> externalFilePath = new AtomicReference<>(null);
                    properties.forEach(diagnosticProperty -> {
                        if (diagnosticProperty.kind().equals(DiagnosticPropertyKind.STRING)) {
                            if (diagnosticProperty.value().equals(ScanToolConstants.CUSTOM_RULE_ID)) {
                                externalIssueRuleID.set((String) diagnosticProperty.value());
                            }

                            if (Path.of((String) diagnosticProperty.value()).toFile().exists()) {
                                externalFilePath.set((String) diagnosticProperty.value());
                            }
                        }
                    });

                    // If all properties are available create a new issue and add to the external issues array
                    if (externalIssueRuleID.get() != null && externalFilePath.get() != null) {
                        Issue newExternalIssue = new Issue(diagnostic.location().lineRange().startLine().line(),
                                diagnostic.location().lineRange().startLine().offset(),
                                diagnostic.location().lineRange().endLine().line(),
                                diagnostic.location().lineRange().endLine().offset(),
                                externalIssueRuleID.get(),
                                diagnostic.message(),
                                issueType,
                                externalFilePath.get());

                        externalIssues.add(newExternalIssue);
                    }
                }
            });

            // Report the external issues
            boolean successfullyReported = issueReporter.reportExternalIssues(externalIssues);
            if (!successfullyReported) {
                System.out.println("Unable to report external issues from: " +
                        currentProject.currentPackage().packageName());
            }
        }
    }
}
