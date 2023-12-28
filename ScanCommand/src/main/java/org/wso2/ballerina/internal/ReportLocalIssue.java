package org.wso2.ballerina.internal;

import io.ballerina.tools.text.LineRange;
import org.wso2.ballerina.Issue;

import java.util.ArrayList;

import static org.wso2.ballerina.CustomScanner.CUSTOM_CHECK_VIOLATION;
import static org.wso2.ballerina.CustomScanner.CUSTOM_RULE_ID;
import static org.wso2.ballerina.internal.InbuiltRules.CUSTOM_RULES;
import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;
import static org.wso2.ballerina.internal.platforms.Local.CHECK_VIOLATION;

public class ReportLocalIssue {
    // Parameters required for custom rules
    private final int SONARQUBE_RESERVED_RULES = 106;
    private int lastRuleIndex = SONARQUBE_RESERVED_RULES + INBUILT_RULES.size();
    private ArrayList<Issue> internalIssues;
    private String issuesFilePath;

    public ReportLocalIssue(ArrayList<Issue> internalIssues, String issuesFilePath) {
        this.internalIssues = internalIssues;
        this.issuesFilePath = issuesFilePath;
    }

    public void reportIssue(LineRange issueLocation, String ruleID, String message) {
        // Only report the issues if it's present locally and activated
        if (INBUILT_RULES.containsKey(ruleID) && INBUILT_RULES.get(ruleID)) {
            reportIssue(issueLocation.startLine().line(),
                    issueLocation.startLine().offset(),
                    issueLocation.endLine().line(),
                    issueLocation.endLine().offset(),
                    ruleID,
                    message,
                    CHECK_VIOLATION,
                    issuesFilePath);
        }
    }

    public void reportIssue(int startLine,
                            int startLineOffset,
                            int endLine,
                            int endLineOffset,
                            String ruleID,
                            String message,
                            String issueType,
                            String issuesFilePath) {
        // Create a new issue
        Issue newIssue = new Issue(startLine,
                startLineOffset,
                endLine,
                endLineOffset,
                ruleID,
                message,
                issueType,
                issuesFilePath
        );

        // Add a new issue
        internalIssues.add(newIssue);
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

                    // verify if customRuleID generation was successful
                    String customRuleID = generateCustomRuleID(message);
                    if (customRuleID == null) {
                        externalIssuesAreValid = false;
                        break;
                    }

                    // Report the external issue
                    reportIssue(startLine,
                            startLineOffset,
                            endLine,
                            endLineOffset,
                            customRuleID,
                            message,
                            issueType,
                            reportedFilePath);
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