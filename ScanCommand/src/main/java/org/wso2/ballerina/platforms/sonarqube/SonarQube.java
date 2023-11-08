package org.wso2.ballerina.platforms.sonarqube;

import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.checks.functionChecks.FunctionChecks;
import org.wso2.ballerina.platforms.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class SonarQube extends Platform {
    // Final attributes for determining the type of issues reported to Sonar Scanner
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    public void scan(String userFile, PrintStream outputStream) {
        // TODO: Instead of printing the JSONArray to output, implement a way to pass it to the sonar bal jar directly
        // Execute the bal scan from the command line
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "sonar-scanner", "-Dsonar.exclusions='**/*.java,**/*.xml,**/*.yaml,**/*.go,**/*.kt,**/*.js,**/*.html,**/*.YAML,**/*.rb,**/*.scala, **/*.py'");
        try {
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outputStream.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        // parse the ballerina file
//        Map<String, Object> compilation = parseBallerinaProject(userFile);
//
//        // perform the static code analysis if the file was successfully parsed
//        if(compilation != null){
//            scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"));
//
//            // The semantic model will be used later when implementing complex rules
//            // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);
//        }
//
//        // Convert the JSON analysis results to the console
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String jsonOutput = gson.toJson(analysisIssues);
//
//        // Output the JSON results to the console
//        outputStream.println(jsonOutput);
    }


    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree) {
        // Function related visits
        FunctionChecks functionChecks = new FunctionChecks(syntaxTree);
        functionChecks.initialize();

        // Other visits
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
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
}
