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
import io.ballerina.projects.Module;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.diagnostics.Location;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InternalReporter {

    private final List<Issue> issues;
    private final Map<Integer, Rule> rules = new HashMap<>();

    InternalReporter(List<Issue> issues, List<Rule> rules) {
        rules.forEach(rule -> {
            this.rules.put(rule.numericId(), rule);
        });
        this.issues = issues;
    }

    void reportIssue(Document reportedDocument, Location location, int ruleId) {
        String documentName = reportedDocument.name();
        Module module = reportedDocument.module();
        String moduleName = module.moduleName().toString();
        Path issuesFilePath = module.project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        // Retrieve the relevant rule from the map
        Rule rule = rules.get(ruleId);
        if (rule == null) {
            throw new NullPointerException();
        }

        // Create a new issue
        IssueIml issue = new IssueIml(location, rule, Source.BUILT_IN,
                moduleName + ScanToolConstants.PATH_SEPARATOR + documentName, issuesFilePath.toString());
        issues.add(issue);
    }

    void reportIssue(Document reportedDocument, Location location, Rule rule) {
        String documentName = reportedDocument.name();
        String moduleName = reportedDocument.module().moduleName().toString();
        Path issuesFilePath = reportedDocument.module().project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        // Create a new Issue
        IssueIml issue = new IssueIml(location, rule, Source.BUILT_IN,
                moduleName + ScanToolConstants.PATH_SEPARATOR + documentName, issuesFilePath.toString());
        issues.add(issue);
    }

    void addExternalIssues(List<Issue> externalIssues) {
        issues.addAll(externalIssues);
    }
}
