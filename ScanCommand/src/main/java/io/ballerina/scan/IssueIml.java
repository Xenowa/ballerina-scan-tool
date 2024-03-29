/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

import io.ballerina.tools.diagnostics.Location;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

public class IssueIml implements Issue {

    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;
    private final String ruleID;
    private final String message;
    private final Source source;
    private final Severity severity;
    // There can be more than one ballerina file which has the same name, so we store it in the following format:
    // fileName = "moduleName/main.bal"
    private final String fileName;
    private final String reportedFilePath;
    private final String reportedSource;

    IssueIml(int startLine,
             int startLineOffset,
             int endLine,
             int endLineOffset,
             String ruleID,
             String message,
             Severity severity,
             Source source,
             String fileName,
             String reportedFilePath,
             String reportedSource) {

        this.startLine = startLine;
        this.startLineOffset = startLineOffset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
        this.ruleID = ruleID;
        this.message = message;
        this.severity = severity;
        this.source = source;
        this.fileName = fileName;
        this.reportedFilePath = reportedFilePath;
        this.reportedSource = reportedSource;
    }

    @Override
    public Location location() {
        return new BLangDiagnosticLocation(fileName, startLine, endLine, startLineOffset, endLineOffset, 0,
                0);
    }

    @Override
    public String ruleId() {
        return ruleID;
    }

    @Override
    public Source source() {
        return source;
    }

    @Override
    public Severity severity() {
        return severity;
    }

    public String getMessage() {
        return message;
    }

    public String getReportedFilePath() {
        return reportedFilePath;
    }

    public String getFileName() {
        return fileName;
    }

    public String getReportedSource() {
        return reportedSource;
    }
}
