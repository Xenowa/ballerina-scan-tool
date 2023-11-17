package org.wso2.ballerina;

import com.google.gson.JsonObject;

public interface Issue {
    JsonObject reportJSONIssue(int startLine
            , int startLineOffset
            , int endLine
            , int endLineOffset
            , String ruleID
            , String message
    );
}
