package balToolTester;

import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Main {
    private static PrintStream outputStream = System.out;
    private static PrintStream errorStream = System.err;

    public static void printFileContents(Path filePath){
        String fileContent = null;
        try{
            fileContent = new String(Files.readAllBytes(filePath)).trim();
        }catch (IOException exception){
            throw new RuntimeException(exception);
        }

        outputStream.println(fileContent);
    }

    public static void tooManyParametersCheck(BLangPackage mainNode, PrintStream outputStream){
        // Obtain all functions from the syntax tree
        List<BLangFunction> functions = mainNode.getFunctions();

        // Only run the rule if the functions are not empty
        if(!functions.isEmpty()){
            functions.forEach(bLangFunction -> {
                // Only trigger the check if there are parameters and the count is greater than 7
                if(!bLangFunction.getParameters().isEmpty() && bLangFunction.getParameters().size() > 7){
                    reportIssue(
                            "CHECK_VIOLATION",
                            bLangFunction.getPosition().lineRange(),
                            "S107",
                            "This function has " + bLangFunction.getParameters().size() + " parameters, which is greater than the 7 authorized.",
                            outputStream
                    );
                }
            });
        }
    }

    public static void reportIssue(String issueType, LineRange issueLocation, String ruleID, String message, PrintStream outputStream){
        // For now we will be displaying the rule violation output directly to the console
        outputStream.println("Displaying Static Code Analysis results:");
        outputStream.println("Issue Type: " + issueType);
        outputStream.println("Start Line: " + issueLocation.startLine().line());
        outputStream.println("Start Line Offset: " + issueLocation.startLine().offset());
        outputStream.println("End Line: " + issueLocation.endLine().line());
        outputStream.println("End Line Offset: " + issueLocation.endLine().offset());
        outputStream.println("Rule ID: " + ruleID);
        outputStream.println(message);
    }

    public static void performStaticAnalysis(Path filePath){
        // Load the Ballerina file
        Project project = ProjectLoader.loadProject(filePath);

        // get the document ID by considering if the project structure is relevant to Ballerina
        DocumentId documentId = project.documentId(filePath);
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            documentId = project.documentId(filePath);
        } else {
            // If project structure is different go to the next document
            Module currentModule = project.currentPackage().getDefaultModule();
            Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();

            // block is used to prevent crashing
            try{
                documentId = documentIterator.next();
            }catch (NoSuchElementException exception){
                errorStream.println(exception);
            }
        }

        // Compile the Ballerina source code file
        PackageCompilation compilation = project.currentPackage().getCompilation();
        // SemanticModel semanticModel = compilation.getSemanticModel(documentId.moduleId());
        // List<Symbol> symbols = semanticModel.visibleSymbols();


        // Retrieve the BLangPackage Node
        BLangPackage bLangPackage = compilation.defaultModuleBLangPackage();

        // Start performing checks
         tooManyParametersCheck(bLangPackage, outputStream);
    }
    public static void main(String[] args) {
        Path balFilePath = Path.of("ScanCommand/bal-scan-tool-tester/main.bal");
        // printFileContents(balFilePath);
        performStaticAnalysis(balFilePath);
    }
}