package org.wso2.ballerina.checks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.PrintStream;
import java.util.List;

import static org.wso2.ballerina.SonarQubeScanner.CHECK_VIOLATION;
import static org.wso2.ballerina.SonarQubeScanner.analysisIssues;

public class FunctionChecks {
    // First Ballerina Specific rule
    public static void tooManyParametersCheck(BLangPackage mainNode){
        // Obtain all functions from the syntax tree
        List<BLangFunction> functions = mainNode.getFunctions();

        // Only run the rule if the functions are not empty
        if(!functions.isEmpty()){
            functions.forEach(bLangFunction -> {
                // Only trigger the check if there are parameters and the count is greater than 7
                if(!bLangFunction.getParameters().isEmpty() && bLangFunction.getParameters().size() > 7){
                    reportIssue(
                            CHECK_VIOLATION,
                            bLangFunction.getPosition().lineRange(),
                            "S107",
                            "This function has " + bLangFunction.getParameters().size() + " parameters, which is greater than the 7 authorized."
                            );
                }
            });
        }
    }

    public static void reportIssue(String issueType, LineRange issueLocation, String ruleID, String message){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("issueType", issueType);
        jsonObject.addProperty("startLine", issueLocation.startLine().line());
        jsonObject.addProperty("startLineOffset", issueLocation.startLine().offset());
        jsonObject.addProperty("endLine", issueLocation.endLine().line());
        jsonObject.addProperty("endLineOffset", issueLocation.endLine().offset());
        jsonObject.addProperty("ruleID", ruleID);
        jsonObject.addProperty("message", message);

        // Convert the JSON output to print to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(jsonObject);

        // add the analysis issue to the issues array
        analysisIssues.add(jsonObject);
    }
}
