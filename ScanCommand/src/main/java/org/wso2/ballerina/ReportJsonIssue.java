package org.wso2.ballerina;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;

public abstract class ReportJsonIssue implements Issue {
    public JsonArray externalIssues = new JsonArray();


    /**
     * For reporting custom rule violations consider using
     * {@link org.wso2.ballerina.ReportJsonIssue#reportIssue(io.ballerina.tools.text.LineRange, String, String)}
     */
    @Override
    public JsonObject reportJSONIssue(int startLine,
                                      int startLineOffset,
                                      int endLine,
                                      int endLineOffset,
                                      String ruleID,
                                      String message) {
        // Create a JSON object to hold the issue results
        JsonObject jsonObject = new JsonObject();

        // Populate the object with the mandatory fields
        jsonObject.addProperty("startLine", startLine);
        jsonObject.addProperty("startLineOffset", startLineOffset);
        jsonObject.addProperty("endLine", endLine);
        jsonObject.addProperty("endLineOffset", endLineOffset);
        jsonObject.addProperty("ruleID", ruleID);
        jsonObject.addProperty("message", message);

        // Return the packaged JSON issue object
        return jsonObject;
    }


    public void reportIssue(LineRange issueLocation, String ruleID, String message) {
        JsonObject jsonObject = reportJSONIssue(issueLocation.startLine().line(),
                issueLocation.startLine().offset(),
                issueLocation.endLine().line(),
                issueLocation.endLine().offset(),
                ruleID,
                message
        );

        // add issue to external issues array
        externalIssues.add(jsonObject);
    }
}