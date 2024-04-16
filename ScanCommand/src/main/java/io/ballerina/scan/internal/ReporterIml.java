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
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.diagnostics.Location;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_ORG;

public class ReporterIml implements Reporter {

    private final List<Issue> issues;

    ReporterIml(List<Issue> issues) {
        this.issues = issues;
    }

    @Override
    public synchronized void reportIssue(Document reportedDocument, Location location, Rule rule) {
        // generate issue reported location information
        String documentName = reportedDocument.name();
        String moduleName = reportedDocument.module().moduleName().toString();
        Path issuesFilePath = reportedDocument.module().project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        // Split the fully qualified id to its source and prefixed ID
        // i.e: org/name:B109
        String fullyQualifiedRuleId = rule.id();
        String[] parts = fullyQualifiedRuleId.split(":");
        String reportedSource = parts[0];

        // Depending on the org name of the compiler plugin set the issue type
        String pluginOrg = reportedSource.split(Pattern.quote(System.getProperty("file.separator")))[0];
        Source source = pluginOrg.equals(BALLERINA_ORG) ? Source.BUILT_IN : Source.EXTERNAL;

        // Construct the issue reported compiler plugin source

        // Create a new Issue
        IssueIml issue = new IssueIml(location, rule, source,
                moduleName + ScanToolConstants.PATH_SEPARATOR + documentName, issuesFilePath.toString());

        // Add the issue reported with the information
        issues.add(issue);
    }

    // TODO: Internal method to be removed once property bag is introduced by project API
    List<Issue> getIssues() {
        List<Issue> existingIssues = new ArrayList<>(issues);
        issues.clear();
        return existingIssues;
    }
}

