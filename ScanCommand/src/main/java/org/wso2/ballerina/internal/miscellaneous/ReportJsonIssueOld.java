package org.wso2.ballerina.internal.miscellaneous;

import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;

import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;

public abstract class ReportJsonIssueOld implements DeprecatedIssue {
    private String issueType;
    private final String ruleID;
    private boolean ruleIsActive;

    public ReportJsonIssueOld(String ruleID) {
        this.ruleID = ruleID;
        this.ruleIsActive = false;
    }

    protected abstract void triggerCheck();

    // Only trigger the implemented check if it's available in the active ruleset and is active
    protected void activateRule() {
        if (INBUILT_RULES.containsKey(ruleID) && INBUILT_RULES.get(ruleID).ruleIsActivated()) {
            ruleIsActive = true;
            triggerCheck();
        }
    }

    // Only report the issue if the rule id is available in the active ruleset
    protected void reportIssue(String issueType, LineRange issueLocation, String message) {
        this.issueType = issueType;
        reportJSONIssue(issueLocation.startLine().line(),
                issueLocation.startLine().offset(),
                issueLocation.endLine().line(),
                issueLocation.endLine().offset(),
                ruleID,
                message
        );
    }

    @Override
    public JsonObject reportJSONIssue(int startLine,
                                      int startLineOffset,
                                      int endLine,
                                      int endLineOffset,
                                      String ruleID,
                                      String message) {
        JsonObject jsonObject = new JsonObject();

        if (ruleIsActive) {
            jsonObject.addProperty("issueType", issueType);
            jsonObject.addProperty("startLine", startLine);
            jsonObject.addProperty("startLineOffset", startLineOffset);
            jsonObject.addProperty("endLine", endLine);
            jsonObject.addProperty("endLineOffset", endLineOffset);
            jsonObject.addProperty("ruleID", ruleID);
            jsonObject.addProperty("message", message);
        }

        // Return the packaged JSON issue object
        return jsonObject;
    }
}