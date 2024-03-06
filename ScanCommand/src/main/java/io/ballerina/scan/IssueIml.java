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
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;

import java.nio.file.Path;

public class IssueIml implements Issue {

    private final int startLine;
    private final int startLineOffset;
    private final int endLine;
    private final int endLineOffset;
    private final String ruleID;
    private final String message;
    private final String issueType;
    private final String issueSeverity;
    // There can be more than one ballerina file which has the same name, so we store it in the following format:
    // fileName = "moduleName/main.bal"
    private final String fileName;
    private final String reportedFilePath;
    private final String reportedSource;

    public IssueIml(int startLine,
                    int startLineOffset,
                    int endLine,
                    int endLineOffset,
                    String ruleID,
                    String message,
                    String issueType,
                    String issueSeverity,
                    Document reportedDocument,
                    Module reportedModule,
                    Project reportedProject,
                    String reportedSource) {

        String documentName = reportedDocument.name();
        String moduleName = reportedModule.moduleName().toString();
        Path issuesFilePath = reportedProject.documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        this.startLine = startLine;
        this.startLineOffset = startLineOffset;
        this.endLine = endLine;
        this.endLineOffset = endLineOffset;
        this.ruleID = ruleID;
        this.message = message;
        this.issueType = issueType;
        this.issueSeverity = issueSeverity;
        this.fileName = moduleName + "/" + documentName;
        this.reportedFilePath = issuesFilePath.toString();
        this.reportedSource = reportedSource;
    }

    IssueIml(int startLine,
             int startLineOffset,
             int endLine,
             int endLineOffset,
             String ruleID,
             String message,
             String issueType,
             String issueSeverity,
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
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getStartLineOffset() {
        return startLineOffset;
    }

    @Override
    public int getEndLine() {
        return endLine;
    }

    @Override
    public int getEndLineOffset() {
        return endLineOffset;
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
    public String getIssueType() {
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
    public String getIssueSeverity() {
        return issueSeverity;
    }

    @Override
    public String getReportedSource() {
        return reportedSource;
    }
}