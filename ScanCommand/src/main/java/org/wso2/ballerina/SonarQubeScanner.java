package org.wso2.ballerina;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import org.wso2.ballerina.checks.FunctionChecks;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

public class SonarQubeScanner {
    // Final attributes for determining the type of issues reported to Sonar Scanner
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    // TODO: Set up functionality to get access to the Ballerina Semantic Model
    public static void scanWithSemanticModel(SemanticModel semanticModel,PrintStream outputStream){
        outputStream.println(semanticModel.toString());
    }

    public static void scanWithPackageNode(BLangPackage bLangPackage, PrintStream outputStream){
        FunctionChecks.tooManyParametersCheck(bLangPackage, outputStream);
    }

    public static Map<String, Object> parseBallerinaProject(String userFile, Path userFilePath, PrintStream errorStream){
        Project project = null;
        DocumentId documentId = null;
        try{
            // Load the Ballerina file
            project = ProjectLoader.loadProject(userFilePath);

            // get the document ID by considering if the project structure is relevant to Ballerina
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                documentId = project.documentId(userFilePath);
            } else {
                // If project structure is different go to the next document
                Module currentModule = project.currentPackage().getDefaultModule();
                Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();

                // block is used to prevent crashing
                try{
                    documentId = documentIterator.next();
                }catch (NoSuchElementException exception){
                    PackageValidator.reportIssue(userFile, errorStream);
                    return null;
                }
            }

            // Compile the Ballerina source code file
            PackageCompilation compilation = project.currentPackage().getCompilation();
            Map<String, Object> compiledOutputs = new HashMap<>();

            // Get the blangPackage
            compiledOutputs.put("blangPackage", compilation.defaultModuleBLangPackage());

            // Get the semanticModel
            compiledOutputs.put("semanticModel", compilation.getSemanticModel(documentId.moduleId()));

            // Return back the compiled objects
            return compiledOutputs;
        }catch (Exception e){
            PackageValidator.reportIssue(userFile, errorStream);
            return null;
        }
    }

    public static void scan(String userFile, PrintStream outputStream, PrintStream errorStream){
        // get the file path of the user provided ballerina file
        Path userFilePath = Path.of(userFile);

        // parse the ballerina file
        Map<String, Object> compilation = parseBallerinaProject(userFile, userFilePath, errorStream);

        // perform the static code analysis if the file was successfully parsed
        if(compilation != null){
            scanWithPackageNode((BLangPackage) compilation.get("blangPackage"), outputStream);

            // We will be using the semantic model in the later implementations
            // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);
        }
    }
}
