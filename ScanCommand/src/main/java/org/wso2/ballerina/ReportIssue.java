package org.wso2.ballerina;

import java.util.ArrayList;

public abstract class ReportIssue {
    private final ArrayList<Issue> issues;

    public ReportIssue(ArrayList<Issue> issues) {
        this.issues = issues;
    }

    public void reportIssue(Issue issue) {
        issues.add(issue);
    }
}