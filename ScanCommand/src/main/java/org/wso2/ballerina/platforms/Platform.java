package org.wso2.ballerina.platforms;

import com.google.gson.JsonArray;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class Platform {
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    // variable to hold all issues
    public static final JsonArray analysisIssues = new JsonArray();

    public Map<String, Object> parseBallerinaProject(String userFile) {
        // get the file path of the user provided ballerina file
        Path userFilePath = Path.of(userFile);

        try {
            // Map to store the parsed & Compiled outputs
            Map<String, Object> compiledOutputs = new HashMap<>();

            // Load the Ballerina file
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
                    handleParseIssue(userFile);
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
            handleParseIssue(userFile);
            return null;
        }
    }

    abstract public void scan(String userFile, PrintStream outputStream);

    abstract public void scan(PrintStream outputStream);

    abstract public void handleParseIssue(String userFile);
}