package org.wso2.ballerina.internal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerina.ReportJsonIssue;

import java.util.HashMap;
import java.util.Map;

import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;
import static org.wso2.ballerina.internal.platforms.Local.CHECK_VIOLATION;
import static org.wso2.ballerina.internal.platforms.Local.CUSTOM_CHECK_VIOLATION;

public class ReportLocalIssue extends ReportJsonIssue {
    // Parameters required for custom rules
    private final int SONARQUBE_RESERVED_RULES = 106;
    private int lastRuleIndex = SONARQUBE_RESERVED_RULES + INBUILT_RULES.size();
    private Map<String, String> customRules = new HashMap<>();

    public ReportLocalIssue(JsonArray externalIssues) {
        super(externalIssues);
    }

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
            issues.add(jsonObject);
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
                    int startLineOffset = issueObject.get("startLineOffset").getAsInt();
                    int endLine = issueObject.get("endLine").getAsInt();
                    int endLineOffset = issueObject.get("endLineOffset").getAsInt();
                    String message = issueObject.get("message").getAsString();

                    String customRuleID = generateCustomRuleID(message);

                    // verify if customRuleID generation was successful
                    if (customRuleID == null) {
                        externalIssuesAreValid = false;
                    } else {
                        // Since the issueObject may have multiple other parameters an internal one is created
                        JsonObject newExternalIssue = new JsonObject();
                        newExternalIssue.addProperty("startLine", startLine);
                        newExternalIssue.addProperty("startLineOffset", startLineOffset);
                        newExternalIssue.addProperty("endLine", endLine);
                        newExternalIssue.addProperty("endLineOffset", endLineOffset);
                        newExternalIssue.addProperty("ruleID", customRuleID);
                        newExternalIssue.addProperty("message", message);

                        // if all other checks passes, then add the property CUSTOM_CHECK_VIOLATION
                        newExternalIssue.addProperty("issueType", CUSTOM_CHECK_VIOLATION);

                        // add external issue as analysis issue
                        issues.add(newExternalIssue);
                    }
                } catch (Exception e) {
                    externalIssuesAreValid = false;
                    break;
                }
            }
        }

        return externalIssuesAreValid;
    }

    public String generateCustomRuleID(String customRuleMessage) {
        // Check if the custom rule has a valid message
        if (customRuleMessage.isEmpty()) {
            return null;
        }

        // Check if the custom rule message already exists and if so return the rule assigned to the message
        if (customRules.containsKey(customRuleMessage)) {
            return customRules.get(customRuleMessage);
        }

        // Increment the last rule index
        lastRuleIndex++;

        // Create custom rule ID
        String customRuleID = "S" + lastRuleIndex;

        // Put the message mapped with the custom rule ID
        customRules.put(customRuleMessage, customRuleID);

        // Put the new rule ID to the inbuilt rules array
        INBUILT_RULES.put(customRuleID, true);

        // return the ruleID
        return customRuleID;
    }
}