package org.wso2.ballerina;

import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;

import static org.wso2.ballerina.InbuiltRules.INBUILT_RULES;
import static org.wso2.ballerina.platforms.Platform.analysisIssues;
import static org.wso2.ballerina.platforms.Platform.CHECK_VIOLATION;

public class ReportJsonIssue implements Issue {

    public void reportIssue(LineRange issueLocation, String ruleID, String message) {
        reportJSONIssue(issueLocation.startLine().line(),
                issueLocation.startLine().offset(),
                issueLocation.endLine().line(),
                issueLocation.endLine().offset(),
                ruleID,
                message
        );
    }

    @Override
    public void reportJSONIssue(int startLine, int startLineOffset, int endLine, int endLineOffset, String ruleID, String message) {
        // Only trigger the activated rules by the user
        if (INBUILT_RULES.containsKey(ruleID) && INBUILT_RULES.get(ruleID)) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("issueType", CHECK_VIOLATION);
            jsonObject.addProperty("startLine", startLine);
            jsonObject.addProperty("startLineOffset", startLineOffset);
            jsonObject.addProperty("endLine", endLine);
            jsonObject.addProperty("endLineOffset", endLineOffset);
            jsonObject.addProperty("ruleID", ruleID);
            jsonObject.addProperty("message", message);

            // add the analysis issue to the issues array
            analysisIssues.add(jsonObject);
        }
    }
}