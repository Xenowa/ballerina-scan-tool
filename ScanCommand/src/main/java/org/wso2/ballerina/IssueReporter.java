package org.wso2.ballerina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class IssueReporter {
    private ArrayList<Issue> issues;
    private final int SONARQUBE_RESERVED_RULES = 106;
    private static int lastRuleIndex;
    private final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final Map<String, String> CUSTOM_RULES = new HashMap<>();

    public IssueReporter(ArrayList<Issue> issues, int lastRuleIndex) {
        this.issues = issues;
        // SONARQUBE_RESERVED_RULES + INBUILT_RULES.size()
        IssueReporter.lastRuleIndex = SONARQUBE_RESERVED_RULES + lastRuleIndex;
    }

    public void reportIssue(int startLine,
                            int startLineOffset,
                            int endLine,
                            int endLineOffset,
                            String message,
                            String issuesFilePath) {
        // Generate a custom rule ID for the reported issue
        String ruleID = generateCustomRuleID(message);

        if (ruleID != null) {
            // Create a new issue
            Issue newIssue = new Issue(startLine,
                    startLineOffset,
                    endLine,
                    endLineOffset,
                    ruleID,
                    message,
                    CUSTOM_CHECK_VIOLATION,
                    issuesFilePath);

            // Add a new issue
            issues.add(newIssue);
        }
    }

    private String generateCustomRuleID(String customRuleMessage) {
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

        // return the ruleID
        return customRuleID;
    }
}
