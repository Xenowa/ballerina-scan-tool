package org.wso2.ballerina.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerina.ReportJsonIssue;

import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;
import static org.wso2.ballerina.internal.platforms.Platform.CHECK_VIOLATION;
import static org.wso2.ballerina.internal.platforms.Platform.analysisIssues;

public class ReportLocalIssue extends ReportJsonIssue {

    @Override
    public void reportIssue(LineRange issueLocation, String ruleID, String message) {
        // Only report the issues if it's present locally and activated
        if (INBUILT_RULES.containsKey(ruleID) && INBUILT_RULES.get(ruleID)) {
            JsonObject jsonObject = reportJSONIssue(issueLocation.startLine().line(),
                    issueLocation.startLine().offset(),
                    issueLocation.endLine().line(),
                    issueLocation.endLine().offset(),
                    ruleID,
                    message
            );

            // set the type of issue, weather it's a parse issue or a rule violation
            jsonObject.addProperty("issueType", CHECK_VIOLATION);
            analysisIssues.add(jsonObject);
        }
    }

    public boolean reportExternalIssues(JsonArray externalIssues) {
        // validate the external issues reported
        boolean externalIssuesAreValid = true;
        for (JsonElement externalIssue : externalIssues) {
            JsonObject issueObject = externalIssue.getAsJsonObject();
            if (issueObject.isEmpty()) {
                externalIssuesAreValid = false;
                break;
            } else {
                try {
                    // retrieve all object keys and validate if they are correct
                    int startLine = issueObject.get("startLine").getAsInt();
                    int startLineOffeset = issueObject.get("startLineOffset").getAsInt();
                    int endLine = issueObject.get("endLine").getAsInt();
                    int endLineOffset = issueObject.get("endLineOffset").getAsInt();
                    String ruleID = issueObject.get("ruleID").getAsString();
                    String message = issueObject.get("message").getAsString();

                    // if all other checks passes, then add the property CHECK_VIOLATION
                    issueObject.addProperty("issueType", CHECK_VIOLATION);

                    // add external issue as analysis issue
                    analysisIssues.add(issueObject);
                } catch (Exception e) {
                    externalIssuesAreValid = false;
                    break;
                }
            }
        }

        return externalIssuesAreValid;
    }
}