package org.wso2.ballerina.checks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;

import static org.wso2.ballerina.SonarQubeScanner.analysisIssues;

public abstract class ReportJsonIssue implements Issue{
    public String issueType;

    public void reportIssue(String issueType, LineRange issueLocation, String ruleID, String message){
        this.issueType = issueType;
        reportSonarIssue(issueLocation.startLine().line(),
                issueLocation.startLine().offset(),
                issueLocation.endLine().line(),
                issueLocation.endLine().offset(),
                ruleID,
                message
        );
    }

    @Override
    public void reportSonarIssue(int startLine, int startLineOffset, int endLine, int endLineOffset, String ruleID, String message) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("issueType", issueType);
        jsonObject.addProperty("startLine", startLine);
        jsonObject.addProperty("startLineOffset", startLineOffset);
        jsonObject.addProperty("endLine", endLine);
        jsonObject.addProperty("endLineOffset", endLineOffset);
        jsonObject.addProperty("ruleID", ruleID);
        jsonObject.addProperty("message", message);

        // Convert the JSON output to print to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(jsonObject);

        // add the analysis issue to the issues array
        analysisIssues.add(jsonObject);
    }
}