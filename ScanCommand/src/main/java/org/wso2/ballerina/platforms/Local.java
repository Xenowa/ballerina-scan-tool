package org.wso2.ballerina.platforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.StaticCodeAnalyzer;
import org.wso2.ballerina.checks.functionChecks.FunctionChecks;

import java.io.PrintStream;
import java.util.Map;

public class Local extends Platform {
    @Override
    public void scan(PrintStream outputStream) {
    }

    @Override
    public void scan(String userFile, PrintStream outputStream) {
        // parse the ballerina file
        Map<String, Object> compilation = parseBallerinaProject(userFile);

        // perform the static code analysis if the file was successfully parsed
        if (compilation != null) {
            scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"));

            // Deprecated scan with syntax tree
            // scanWithSyntaxTreeOld((SyntaxTree) compilation.get("syntaxTree"));

            // The semantic model will be used later when implementing complex rules
            // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);
        }

        // Convert the JSON analysis results to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(analysisIssues);
        outputStream.println(jsonOutput);
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize();
    }

    public void scanWithSyntaxTreeOld(SyntaxTree syntaxTree) {
        // Function relaated visits
        FunctionChecks functionChecks = new FunctionChecks(syntaxTree);
        functionChecks.initialize();
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
    }

    @Override
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
