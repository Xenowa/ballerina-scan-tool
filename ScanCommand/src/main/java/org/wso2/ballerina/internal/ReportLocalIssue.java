package org.wso2.ballerina.internal;

import io.ballerina.tools.text.LineRange;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.ReportIssue;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.wso2.ballerina.CustomScanner.CUSTOM_CHECK_VIOLATION;
import static org.wso2.ballerina.CustomScanner.CUSTOM_RULE_ID;
import static org.wso2.ballerina.internal.InbuiltRules.CUSTOM_RULES;
import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;
import static org.wso2.ballerina.internal.platforms.Local.CHECK_VIOLATION;

public class ReportLocalIssue extends ReportIssue {
    // Parameters required for custom rules
    private final int SONARQUBE_RESERVED_RULES = 106;
    private int lastRuleIndex = SONARQUBE_RESERVED_RULES + INBUILT_RULES.size();

    public ReportLocalIssue(ArrayList<Issue> internalIssues) {
        super(internalIssues);
    }

    public void reportIssue(LineRange issueLocation, String ruleID, String message, String reportedFilePath) {
        // Only report the issues if it's present locally and activated
        if (INBUILT_RULES.containsKey(ruleID) && INBUILT_RULES.get(ruleID)) {
            Issue inbuiltIssue = new Issue(issueLocation.startLine().line(),
                    issueLocation.startLine().offset(),
                    issueLocation.endLine().line(),
                    issueLocation.endLine().offset(),
                    ruleID,
                    message,
                    CHECK_VIOLATION,
                    Path.of(reportedFilePath).toAbsolutePath().toString());

            // Add the issue to the issues array
            reportIssue(inbuiltIssue);
        }
    }

    public boolean reportExternalIssues(ArrayList<Issue> externalIssues) {
        // validate the external issues reported
        boolean externalIssuesAreValid = true;

        // Transfer external issues to the internal issues array
        for (Issue externalIssue : externalIssues) {
            if (externalIssue == null) {
                externalIssuesAreValid = false;
                break;
            } else {
                try {
                    // Check the validity of the issue reported
                    String ruleID = externalIssue.getRuleID();
                    String issueType = externalIssue.getIssueType();
                    if (!(ruleID.equals(CUSTOM_RULE_ID) && issueType.equals(CUSTOM_CHECK_VIOLATION))) {
                        externalIssuesAreValid = false;
                        break;
                    }

                    int startLine = externalIssue.getStartLine();
                    int startLineOffset = externalIssue.getStartLineOffset();
                    int endLine = externalIssue.getEndLine();
                    int endLineOffset = externalIssue.getEndLineOffset();
                    String message = externalIssue.getMessage();
                    String reportedFilePath = externalIssue.getReportedFilePath();

                    String customRuleID = generateCustomRuleID(message);
                    // verify if customRuleID generation was successful
                    if (customRuleID == null) {
                        externalIssuesAreValid = false;
                        break;
                    }

                    // Create a new external issue
                    Issue newExternalIssue = new Issue(startLine,
                            startLineOffset,
                            endLine,
                            endLineOffset,
                            customRuleID,
                            message,
                            issueType,
                            Path.of(reportedFilePath).toAbsolutePath().toString());

                    // Add the external issue to the internal issues array
                    reportIssue(newExternalIssue);
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
        if (CUSTOM_RULES.containsKey(customRuleMessage)) {
            return CUSTOM_RULES.get(customRuleMessage);
        }

        // Increment the last rule index
        lastRuleIndex++;

        // Create custom rule ID
        String customRuleID = "S" + lastRuleIndex;

        // Put the message mapped with the custom rule ID
        CUSTOM_RULES.put(customRuleMessage, customRuleID);

        // Put the new rule ID to the inbuilt rules array
        INBUILT_RULES.put(customRuleID, true);

        // return the ruleID
        return customRuleID;
    }
}