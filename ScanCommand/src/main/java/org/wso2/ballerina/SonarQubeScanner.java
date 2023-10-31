package org.wso2.ballerina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import org.wso2.ballerina.checks.FunctionChecks;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class SonarQubeScanner{
    // Final attributes for determining the type of issues reported to Sonar Scanner
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";
    public static final JsonArray analysisIssues = new JsonArray();

    public Map<String, Object> parseBallerinaProject(String userFile, Path userFilePath){
        // For reporting parsing issues
        PackageValidator packageValidator = new PackageValidator();

        try{
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
                try{
                    documentId = documentIterator.next();
                }catch (NoSuchElementException exception){
                    packageValidator.reportIssue(userFile);
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
        }catch (Exception e){
            packageValidator.reportIssue(userFile);
            return null;
        }
    }

    public void scan(String userFile, PrintStream outputStream){
        // get the file path of the user provided ballerina file
        Path userFilePath = Path.of(userFile);

        // parse the ballerina file
        Map<String, Object> compilation = parseBallerinaProject(userFile, userFilePath);

        // perform the static code analysis if the file was successfully parsed
        if(compilation != null){
            scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"));

            // The semantic model will be used later when implementing complex rules
            // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"));
        }

        // Convert the JSON analysis results to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(analysisIssues);
        outputStream.println(jsonOutput);
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree){
        // Function related visits
        FunctionChecks functionChecks = new FunctionChecks(syntaxTree);
        functionChecks.initialize();

        // Other visits
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel,PrintStream outputStream){
        outputStream.println(semanticModel.toString());
    }
}
