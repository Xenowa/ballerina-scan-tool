package org.wso2.ballerina.internal.miscellaneous.platforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.internal.Reporter;
import org.wso2.ballerina.internal.StaticCodeAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeprecatedLocal {
    // Internal Issue type
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";

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

        // Get the blangPackage also known as AST (will be deprecated in the future)
        // compiledOutputs.put("blangPackage", compilation.defaultModuleBLangPackage());

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


            // pass the issue reporter to perform the analysis issue reporting through the analyzers
            scanWithSyntaxTree((SyntaxTree) compiledOutputs.get("syntaxTree"), issueReporter);

            // The semantic model will be used later when implementing complex rules
            // scanWithSemanticModel((SemanticModel) compiledOutputs.get("semanticModel"), outputStream);

            // External rule plugins analysis
            // ========
            // METHOD 1 (using tool plugins with ServiceLoaders)
            // ========
            // - User has to move JAR's to be located in same directory where tool is located
//        ServiceLoader<ExternalRules> externalRulesJars = ServiceLoader.load(ExternalRules.class);
//        // Iterate through the loaded interfaces
//        for (ExternalRules externalRulesJar : externalRulesJars) {
//            // Call the initialize method to trigger custom rule scans
//            externalRulesJar.initialize(
//                    (SyntaxTree) compiledOutputs.get("syntaxTree"),
//                    (SemanticModel) compiledOutputs.get("semanticModel")
//            );
//
//            // Retrieve the externalIssues created by each custom rules JAR
//            JsonArray externalIssuesArray = externalRulesJar.getExternalIssues();
//
//            // Then report the external issues
//            boolean successfullyReported = issueReporter.reportExternalIssues(externalIssuesArray);
//
//            if (!successfullyReported) {
//                handleParseIssue("Unable to load custom rules, issues reported are having invalid format!");
//            }
//        }

            // ========
            // METHOD 2 (using compiler plugins with diagnostics)
            // ========
            // - User does not have to manually move JAR's everywhere
            // Running Compiler plugins through compiling imports of the project
            PackageCompilation engagedPlugins = currentProject.currentPackage().getCompilation();

            // We are able to access the diagnostics saved in the compiler plugins and next add them to the issues array
            ArrayList<Issue> externalIssues = new ArrayList<>();

            engagedPlugins.diagnosticResult().diagnostics().forEach(diagnostic -> {
                String issueType = diagnostic.diagnosticInfo().code();

                if (issueType.equals("CUSTOM_CHECK_VIOLATION")) {
                    List<DiagnosticProperty<?>> properties = diagnostic.properties();

                    // Retrieve the Issue property and add it to the issues array
                    properties.forEach(diagnosticProperty -> {
                        if (diagnosticProperty.value() instanceof Issue) {
                            Issue externalIssue = (Issue) diagnosticProperty.value();
                            externalIssues.add(externalIssue);
                        }
                    });
                }
            });

            // Report the external issues
            issueReporter.reportExternalIssues(externalIssues);
        }

        // If there are user picked rules, then return a filtered issues array
//        if (!userRule.equals("all")) {
//            // Scanned results will be empty if the rule does not exist
//            ArrayList<Issue> filteredInternalIssues = new ArrayList<>();
//
//            if (InbuiltRules.INBUILT_RULES.containsKey(userRule)) {
//                for (Issue internalIssue : internalIssues) {
//                    if (internalIssue.getRuleID().equals(userRule)) {
//                        filteredInternalIssues.add(internalIssue);
//                    }
//                }
//            }
//
//            return filteredInternalIssues;
//        }

        // Return the analyzed file results
        return internalIssues;
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree, Reporter issueReporter) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize(issueReporter);
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
    }

    // Save results to file
    public String saveResults(ArrayList<Issue> issues) {
        // Convert the output to a string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        String jsonOutput = gson.toJson(issuesAsJson);

        // Save analysis results to file
        File newTempFile;
        try {
            newTempFile = new File("ballerina-analysis-results.json");

            // Create a new file to hold analysis results
            newTempFile.createNewFile();

            // write the analysis results to the new file
            FileWriter writer = new FileWriter(newTempFile);
            writer.write(jsonOutput);
            writer.close();

        } catch (IOException e) {
            return null;
        }

        return newTempFile.getAbsolutePath();
    }
}
