package org.wso2.ballerina.internal.platforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.wso2.ballerina.ExternalRules;
import org.wso2.ballerina.internal.ReportLocalIssue;
import org.wso2.ballerina.internal.StaticCodeAnalyzer;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.ServiceLoader;

public class Local {
    // Issue type
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    public static final JsonArray analysisIssues = new JsonArray();

    // For parsing individual ballerina files
    public Map<String, Object> parseBallerinaFile(Path userFilePath) {
        try {
            // Map to store the parsed & Compiled outputs
            Map<String, Object> compiledOutputs = new HashMap<>();

            Project project = ProjectLoader.loadProject(userFilePath);

            // Retrieve the main module of the Ballerina Project
            Module currentModule = project.currentPackage().getDefaultModule();

            // Get the document ID by considering if the project structure is relevant to Ballerina
            DocumentId documentId;
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                documentId = project.documentId(userFilePath);
            } else {
                Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();

                // Block is used to prevent crashing
                try {
                    documentId = documentIterator.next();
                } catch (NoSuchElementException exception) {
                    handleParseIssue(userFilePath.getFileName().toString());
                    return null;
                }
            }

            // Get the the user file as a ballerina document
            Document document = currentModule.document(documentId);

            // Retrieve the syntax tree from the ballerina document
            compiledOutputs.put("syntaxTree", document.syntaxTree());

            // Compile the Ballerina source code file
            PackageCompilation compilation = project.currentPackage().getCompilation();

            // Get the blangPackage or the AST (will be deprecated in the future)
            // compiledOutputs.put("blangPackage", compilation.defaultModuleBLangPackage());

            // Retrieve the semantic model from the ballerina document compilation
            compiledOutputs.put("semanticModel", compilation.getSemanticModel(documentId.moduleId()));

            // Return back the compiled objects
            return compiledOutputs;
        } catch (Exception e) {
            handleParseIssue(userFilePath.getFileName().toString());
            return null;
        }
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

    /*
     * TODO:
     *  Method which parses the ballerina Projects and returns the per file analysis results triggered from
     *  "bal scan --platform=sonarqube"
     * */
    public JsonArray analyzeProject(Path projectFolderPath) {
        // Array to hold all analyzed file results
        JsonArray analyzedFiles = new JsonArray();

        // For now we will not be considering external rules and proceed with performing scans here itself
        // Get access to the project API
        Project project = ProjectLoader.loadProject(projectFolderPath);

        // Iterate through each module of the project
        project.currentPackage().moduleIds().forEach(moduleId -> {
            // Get access to the project modules
            Module module = project.currentPackage().module(moduleId);

            // Iterate through each document of the module
            module.documentIds().forEach(documentId -> {
                // Object to hold document scanned results
                JsonObject analyzedFile = new JsonObject();
                // Retrieve each document from the module
                Document document = module.document(documentId);

                // ==================================================
                // Perform the static code analysis for each document
                // ==================================================
                // Retrieve the absolute path of the document and put it to the file object
                Optional<Path> documentPath = project.documentPath(documentId);
                analyzedFile.addProperty("ballerinaFilePath", documentPath.get().toAbsolutePath().toString());

                // Map to store the parsed & Compiled outputs
                Map<String, Object> compiledOutputs = new HashMap<>();

                // Retrieve the syntax tree from the parsed ballerina document
                compiledOutputs.put("syntaxTree", document.syntaxTree());

                // Retrieve the compilation of the module
                ModuleCompilation compilation = module.getCompilation();

                // Get the blangPackage also know as AST (will be deprecated in the future)
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

                // Put the analyzed results array to the analyzed file object
                analyzedFile.add("reportedIssues", reportedIssues);

                // Add the analyzed file results to the Json
                analyzedFiles.add(analyzedFile);
            });
        });

        // return the analyzed files
        return analyzedFiles;
    }

    public void scan(String userFile, PrintStream outputStream) {
        // Set up the issue reporter here so that external issues also can be included
        ReportLocalIssue issueReporter = new ReportLocalIssue(analysisIssues);

        // parse the ballerina file
        Map<String, Object> compilation = parseBallerinaFile(Path.of(userFile));

        // perform the static code analysis if the file was successfully parsed
        // Load all JAR's that implement the external rules interface (Using Java SPI)
        // This will only load classes from the JAR files located in the directory where
        // the JAR that triggers it is in
        ServiceLoader<ExternalRules> externalRulesJars = ServiceLoader.load(ExternalRules.class);
        // Iterate through the loaded interfaces
        for (ExternalRules externalRulesJar : externalRulesJars) {
            // Call the initialize method to trigger custom rule scans
            externalRulesJar.initialize(
                    (SyntaxTree) compilation.get("syntaxTree"),
                    (SemanticModel) compilation.get("semanticModel")
            );

            // Retrieve the externalIssues created by each custom rules JAR
            JsonArray externalIssuesArray = externalRulesJar.getExternalIssues();

            // Then report the external issues
            boolean successfullyReported = issueReporter.reportExternalIssues(externalIssuesArray);

            if (!successfullyReported) {
                handleParseIssue("Unable to load custom rules, issues reported are having invalid format!");
            }
        }

        scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"), issueReporter);

        // The semantic model will be used later when implementing complex rules
        // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);

        // Convert the JSON analysis results to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(analysisIssues);
        outputStream.println(jsonOutput);
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
}
