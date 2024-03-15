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
import io.ballerina.scan.utilities.ScanToolConstants;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;

import java.nio.file.Path;
import java.util.ArrayList;

public class InternalReporter {

    private static final String BALLERINA_RULE_PREFIX = "S";

    private final ArrayList<Issue> issues;
    private final String reportedSource;

    InternalReporter(ArrayList<Issue> issues, String reportedSource) {

        this.issues = issues;
        this.reportedSource = reportedSource;
    }

    void reportIssue(Document reportedDocument, Location location, Rule rule) {
        String documentName = reportedDocument.name();
        String moduleName = reportedDocument.module().moduleName().toString();
        Path issuesFilePath = reportedDocument.module().project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        LineRange lineRange = location.lineRange();

        // Create a new Issue
        IssueIml issue = new IssueIml(lineRange.startLine().line(),
                lineRange.startLine().offset(),
                lineRange.endLine().line(),
                lineRange.endLine().offset(),
                BALLERINA_RULE_PREFIX + rule.getId(), // Generate the prefix when reporting the issue
                rule.getDescription(),
                rule.getSeverity(),
                IssueType.CORE_ISSUE,
                moduleName + ScanToolConstants.PATH_SEPARATOR + documentName,
                issuesFilePath.toString(),
                reportedSource);

        issues.add(issue);
    }

    void addExternalIssues(ArrayList<Issue> externalIssues) {

        issues.addAll(externalIssues);
    }
}
