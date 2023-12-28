package org.wso2.ballerina;

import java.util.ArrayList;

public abstract class ReportExternalIssue {
    private final ArrayList<Issue> issues;
    private final String issuesFilePath;

    public ReportExternalIssue(ArrayList<Issue> issues, String issuesFilePath) {
        this.issues = issues;
        this.issuesFilePath = issuesFilePath;
    }

    public void reportIssue(int startLine,
                            int startLineOffset,
                            int endLine,
                            int endLineOffset,
                            String ruleID,
                            String message,
                            String issueType) {
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
        issues.add(newIssue);
    }
}