package org.wso2.ballerina;

public class Issue {
    private int startLine;
    private int startLineOffset;
    private int endLine;
    private int endLineOffset;
    private String ruleID;
    private String message;
    private String issueType;
    private String reportedFilePath;

    public Issue(int startLine,
                 int startLineOffset,
                 int endLine,
                 int endLineOffset,
                 String ruleID,
                 String message,
                 String issueType,
                 String reportedFilePath) {
        this.startLine = startLine;
        this.startLineOffset = startLineOffset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
        this.ruleID = ruleID;
        this.message = message;
        this.issueType = issueType;
        this.reportedFilePath = reportedFilePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartLineOffset() {
        return startLineOffset;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndLineOffset() {
        return endLineOffset;
    }

    public String getRuleID() {
        return ruleID;
    }

    public String getMessage() {
        return message;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getReportedFilePath() {
        return reportedFilePath;
    }
}