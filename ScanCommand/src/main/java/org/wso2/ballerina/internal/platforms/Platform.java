package org.wso2.ballerina.internal.platforms;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public abstract class Platform {
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    // variable to hold all issues
    public static final JsonArray analysisIssues = new JsonArray();

    // We need to send the absolute path of the current initiating project of bal scan here
    public Map<String, Object> parseBallerinaProject(String filePath) {
        // TODO: Requirements from the bal scan tool:
        //  =========================================
        //  - basically the bal scan should be similar to bal build:
        //      - bal build works with absolute/relative path of ballerina project created using the bal new command
        //      - bal build also works when it's executed inside of a build project without needing the absolute
        //      or relative paths, this is done by reading the toml files inside of the build project
        //  - To access the ballerina semantic model, a project should be loaded and a single ballerina file
        //  cannot be utilized
        //  - It does not need to scan nested ballerina projects in the working directory, only the first level
        //  ballerina project folders

        // TODO: How the bal scan tool should work:
        //  =======================================
        //  - The absolute path of each build project in the working directory should be provided to the bal scan
        //  tool
        //  - A way should be implemented in the sonar-ballerina plugin to provide the absolute path of all
        //  directories inside the working directory the "bal scan --platform=sonarqube" was triggered in
        //  - The correct flow should be similar to the following:
        /**
         * TODO: Correct structure of using the ballerina project API:
         *  ==========================================================
         *          // get the project path where the scan was triggered from
         *         Path userProjectPath = Path.of(projectPath);
         *  *
         *         // Arraylist to hold compiledBallerinaFiles
         *         ArrayList<BallerinaFile> compiledBallerinaFiles = new ArrayList<BallerinaFile>();
         *  *
         *         Project project = ProjectLoader.loadProject(userProjectPath);
         *  *
         *         // Iterate through each module of the project
         *         project.currentPackage().moduleIds().forEach(moduleId -> {
         *             Module module = project.currentPackage().module(moduleId);
         *  *
         *             // Iterate through each document of the module
         *             module.documentIds().forEach(documentId -> {
         *                 // The BallerinaFile object creation starts from here
         *                 BallerinaFile ballerinaFile;
         *                 Document document = module.document(documentId);
         *  *
         *                 // ==================================================
         *                 // Perform the static code analysis for each document
         *                 // ==================================================
         *                 // Retrieve the absolute path of the document
         *                 Optional<Path> absolutePath = project.documentPath(documentId);
         *                 ballerinaFile.setAbsolutePath(absolutePath);
         *  *
         *                 // Map to store the parsed & Compiled outputs
         *                 Map<String, Object> compiledOutputs = new HashMap<>();
         *  *
         *                 // Retrieve the syntax tree from the ballerina document
         *                 compiledOutputs.put("syntaxTree", document.syntaxTree());
         *  *
         *                 // Compile the Ballerina source code file
         *                 PackageCompilation compilation = project.currentPackage().getCompilation();
         *  *
         *                 // Get the blangPackage or the AST (will be deprecated in the future)
         *                 // compiledOutputs.put("blangPackage", compilation.defaultModuleBLangPackage());
         *  *
         *                 // Retrieve the semantic model from the ballerina document compilation
         *                 compiledOutputs.put("semanticModel", compilation.getSemanticModel(documentId.moduleId()));
         *  *
         *                 // Return back the compiled objects
         *                 ballerinaFile.setCompiledOutputs(compiledOutputs);
         *  *
         *                 // Put the created Ballerina file to the arraylist of compiledBallerinaFiles
         *                 compiledBallerinaFiles.add(ballerinaFile);
         *             });
         *         });
         *         return compiledBallerinaFiles;
         * */

        // get the path of the user file provided to bal scan
        Path userFilePath = Path.of(filePath);

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
                    handleParseIssue(filePath);
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
            handleParseIssue(filePath);
            return null;
        }
    }

    abstract public void scan(String userFile, PrintStream outputStream);

    abstract public void scan(PrintStream outputStream);

    public void handleParseIssue(String userFile) {
        JsonObject jsonObject = new JsonObject();

        // Create a JSON Object of the error
        jsonObject.addProperty("issueType", SOURCE_INVALID);
        String message = "Unable to parse file " + userFile;
        jsonObject.addProperty("message", message);

        // add the analysis issue to the issues array
        analysisIssues.add(jsonObject);
    }
}