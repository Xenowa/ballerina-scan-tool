/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.internal;

import io.ballerina.projects.Document;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.diagnostics.Location;

import java.nio.file.Path;
import java.util.List;

public class InternalReporter {

    private final List<Issue> issues;

    InternalReporter(List<Issue> issues) {
        this.issues = issues;
    }

    void reportIssue(Document reportedDocument, Location location, Rule rule) {
        String documentName = reportedDocument.name();
        String moduleName = reportedDocument.module().moduleName().toString();
        Path issuesFilePath = reportedDocument.module().project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        // Create a new Issue
        IssueIml issue = new IssueIml(location,
                rule,
                Source.BUILT_IN,
                moduleName + ScanToolConstants.PATH_SEPARATOR + documentName,
                issuesFilePath.toString());

        issues.add(issue);
    }

    void addExternalIssues(List<Issue> externalIssues) {
        issues.addAll(externalIssues);
    }
}
