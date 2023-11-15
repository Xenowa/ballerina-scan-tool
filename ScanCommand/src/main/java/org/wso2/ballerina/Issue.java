package org.wso2.ballerina;

public interface Issue {
    void reportJSONIssue(int startLine
            , int startLineOffset
            , int endLine
            , int endLineOffset
            , String ruleID
            , String message
    );
}
