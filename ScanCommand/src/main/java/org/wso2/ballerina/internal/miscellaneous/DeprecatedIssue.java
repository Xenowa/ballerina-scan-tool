package org.wso2.ballerina.internal.miscellaneous;

import com.google.gson.JsonObject;

public interface DeprecatedIssue {
    JsonObject reportJSONIssue(int startLine
            , int startLineOffset
            , int endLine
            , int endLineOffset
            , String ruleID
            , String message
    );
}