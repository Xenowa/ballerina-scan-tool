package org.wso2.ballerina.internal.platforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import org.wso2.ballerina.ExternalRules;
import org.wso2.ballerina.internal.InbuiltRules;
import org.wso2.ballerina.internal.ReportLocalIssue;
import org.wso2.ballerina.internal.StaticCodeAnalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import static org.wso2.ballerina.internal.ScanCommand.userRule;

public class Local {
    // Issue type
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";
    public static final JsonArray analysisIssues = new JsonArray();

    public JsonObject analyzeDocument(Project currentProject, Module currentModule, DocumentId documentId) {
        // Object to hold document scanned results
        JsonObject analyzedFile = new JsonObject();

        // Retrieve each document from the module
        Document document = currentModule.document(documentId);

        // Retrieve the absolute path of the document and put it to the file object
        Optional<Path> documentPath = currentProject.documentPath(documentId);
        analyzedFile.addProperty("ballerinaFilePath", documentPath.get().toAbsolutePath().toString());

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
        // Array to hold analysis results for each document
        JsonArray reportedIssues = new JsonArray();

        // Set up the issue reporter here so issues can be reported from the static code analyzers
        // in a proper format
        ReportLocalIssue issueReporter = new ReportLocalIssue(reportedIssues);

        // External rule plugins analysis
        ServiceLoader<ExternalRules> externalRulesJars = ServiceLoader.load(ExternalRules.class);
        // Iterate through the loaded interfaces
        for (ExternalRules externalRulesJar : externalRulesJars) {
            // Call the initialize method to trigger custom rule scans
            externalRulesJar.initialize(
                    (SyntaxTree) compiledOutputs.get("syntaxTree"),
                    (SemanticModel) compiledOutputs.get("semanticModel")
            );

            // Retrieve the externalIssues created by each custom rules JAR
            JsonArray externalIssuesArray = externalRulesJar.getExternalIssues();

            // Then report the external issues
            boolean successfullyReported = issueReporter.reportExternalIssues(externalIssuesArray);

            if (!successfullyReported) {
                handleParseIssue("Unable to load custom rules, issues reported are having invalid format!");
            }
        }

        // pass the issue reporter to perform the analysis issue reporting through the analyzers
        scanWithSyntaxTree((SyntaxTree) compiledOutputs.get("syntaxTree"), issueReporter);

        // The semantic model will be used later when implementing complex rules
        // scanWithSemanticModel((SemanticModel) compiledOutputs.get("semanticModel"), outputStream);

        // Filter the reportedIssues and select only the issues relevant to the user provided rule if any
        if (!userRule.equals("all")) {
            // Scanned results will be empty if the rule does not exist
            JsonArray filteredIssues = new JsonArray();

            if (InbuiltRules.INBUILT_RULES.containsKey(userRule)) {
                for (JsonElement reportedIssue : reportedIssues) {
                    JsonObject reportedIssueObject = reportedIssue.getAsJsonObject();

                    if (reportedIssueObject.get("ruleID").getAsString().equals(userRule)) {
                        filteredIssues.add(reportedIssueObject);
                    }
                }
            }

            analyzedFile.add("reportedIssues", filteredIssues);
        } else {
            // Put the analyzed results array to the analyzed file object
            analyzedFile.add("reportedIssues", reportedIssues);
        }

        // Return the analyzed file results
        return analyzedFile;
    }

    public JsonArray analyzeProject(Path userPath) {
        // Array to hold all analyzed file results
        JsonArray analyzedFiles = new JsonArray();

        // Get access to the project API
        Project project = ProjectLoader.loadProject(userPath);

        // For single file inputs with bal scan
        if (!userPath.toFile().isDirectory()) {
            // if the user provided file path belongs to a build project stop the analysis
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                return analyzedFiles;
            }

            Module tempModule = project.currentPackage().getDefaultModule();
            DocumentId documentId = project.documentId(userPath);
            JsonObject analyzedDocument = analyzeDocument(project, tempModule, documentId);
            analyzedFiles.add(analyzedDocument);
        } else {
            // Iterate through each module of the project
            project.currentPackage().moduleIds().forEach(moduleId -> {
                // Get access to the project modules
                Module module = project.currentPackage().module(moduleId);

                // Iterate through each ballerina test file in a ballerina project and perform static analysis
                module.testDocumentIds().forEach(testDocumentID -> {
                    JsonObject analyzedDocument = analyzeDocument(project, module, testDocumentID);
                    analyzedFiles.add(analyzedDocument);
                });

                // Iterate through each document of the Main module/project + sub modules
                module.documentIds().forEach(documentId -> {
                    JsonObject analyzedDocument = analyzeDocument(project, module, documentId);
                    analyzedFiles.add(analyzedDocument);
                });
            });
        }

        // return the analyzed files
        return analyzedFiles;
    }

    public void handleParseIssue(String userFile) {
        JsonObject jsonObject = new JsonObject();

        // Create a JSON Object of the error
        jsonObject.addProperty("issueType", SOURCE_INVALID);
        String message = "Unable to parse file " + userFile;
        jsonObject.addProperty("message", message);

        // add the analysis issue to the issues array
        analysisIssues.add(jsonObject);
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree, ReportLocalIssue issueReporter) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize(issueReporter);
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
    }

    // Save results to file
    public String saveResults(JsonArray scannedResults) {
        // Convert the output to a string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scannedResults);

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
