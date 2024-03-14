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

import io.ballerina.projects.Document;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.nio.file.Path;

import static io.ballerina.scan.utilities.ScanToolConstants.PATH_SEPARATOR;

public class IssueIml implements Issue {

    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;
    private final String ruleID;
    private final String message;
    private IssueType issueType;
    private final Severity issueSeverity;
    // There can be more than one ballerina file which has the same name, so we store it in the following format:
    // fileName = "moduleName/main.bal"
    private final String fileName;
    private final String reportedFilePath;
    private String reportedSource;

    public IssueIml(Location location,
                    Rule rule,
                    Document reportedDocument) {

        String documentName = reportedDocument.name();
        String moduleName = reportedDocument.module().moduleName().toString();
        Path issuesFilePath = reportedDocument.module().project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        LineRange lineRange = location.lineRange();
        this.startLine = lineRange.startLine().line();
        this.startLineOffset = lineRange.startLine().offset();
        this.endLine = lineRange.endLine().line();
        this.endLineOffset = lineRange.endLine().offset();
        this.ruleID = rule.getRuleID();
        this.message = rule.getRuleDescription();
        this.issueSeverity = rule.getRuleSeverity();
        this.fileName = moduleName + PATH_SEPARATOR + documentName;
        this.reportedFilePath = issuesFilePath.toString();
    }

    IssueIml(int startLine,
             int startLineOffset,
             int endLine,
             int endLineOffset,
             String ruleID,
             String message,
             IssueType issueType,
             Severity issueSeverity,
             String fileName,
             String reportedFilePath,
             String reportedSource) {

        this.startLine = startLine;
        this.startLineOffset = startLineOffset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
        this.ruleID = ruleID;
        this.message = message;
        this.issueType = issueType;
        this.issueSeverity = issueSeverity;
        this.fileName = fileName;
        this.reportedFilePath = reportedFilePath;
        this.reportedSource = reportedSource;
    }

    @Override
    public Location getLocation() {
        return new BLangDiagnosticLocation(fileName, startLine, endLine, startLineOffset, endLineOffset, 0,
                0);
    }

    @Override
    public String getRuleID() {
        return ruleID;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public IssueType getIssueType() {
        return issueType;
    }

    @Override
    public String getReportedFilePath() {
        return reportedFilePath;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public Severity getIssueSeverity() {
        return issueSeverity;
    }

    @Override
    public String getReportedSource() {
        return reportedSource;
    }

    void setReportedSource(String reportedSource) {
        this.reportedSource = reportedSource;
    }

    void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }
}
