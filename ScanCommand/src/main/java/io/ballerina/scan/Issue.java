package io.ballerina.scan;

public class Issue {

    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;
    private final String ruleID;
    private final String message;
    private final String issueType;
    private final String type;
    // There can be more than one ballerina file which has the same name, so we store it in the following format:
    // fileName = "moduleName/main.bal"
    private final String fileName;
    private final String reportedFilePath;

    public Issue(int startLine,
                 int startLineOffset,
                 int endLine,
                 int endLineOffset,
                 String ruleID,
                 String message,
                 String issueType,
                 String type,
                 String fileName,
                 String reportedFilePath) {

        this.startLine = startLine;
        this.startLineOffset = startLineOffset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
        this.ruleID = ruleID;
        this.message = message;
        this.issueType = issueType;
        this.type = type;
        this.fileName = fileName;
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

    public String getFileName() {

        return fileName;
    }

    public String getType() {

        return type;
    }
}
