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
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.diagnostics.Location;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_ORG;
import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_RULE_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.PATH_SEPARATOR;

public class ReporterIml implements Reporter {

    private final List<Issue> issues = new ArrayList<>();
    private final Map<Integer, Rule> rules = new HashMap<>();

    ReporterIml(List<Rule> rules) {
        rules.forEach(rule -> {
            this.rules.put(rule.numericId(), rule);
        });
    }

    @Override
    public void reportIssue(Document reportedDocument, Location location, int ruleId) {
        String documentName = reportedDocument.name();
        Module module = reportedDocument.module();
        String moduleName = module.moduleName().toString();
        Path issuesFilePath = module.project().documentPath(reportedDocument.documentId())
                .orElse(Path.of(documentName));

        Rule rule = rules.get(ruleId);
        if (rule == null) {
            throw new RuntimeException();
        }

        String fullyQualifiedRuleId = rule.id();
        String[] parts = fullyQualifiedRuleId.split(":");
        Source source;
        if (parts[0].equals(BALLERINA_RULE_PREFIX + rule.numericId())) {
            source = Source.BUILT_IN;
        } else {
            String reportedSource = parts[0];
            String pluginOrg = reportedSource.split(PATH_SEPARATOR)[0];
            source = pluginOrg.equals(BALLERINA_ORG) ? Source.BUILT_IN : Source.EXTERNAL;
        }

        Issue issue = new IssueIml(location, rule, source,
                moduleName + System.lineSeparator() + documentName, issuesFilePath.toString());
        issues.add(issue);
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
        Issue issue = new IssueIml(location, rule, source,
                moduleName + ScanToolConstants.PATH_SEPARATOR + documentName, issuesFilePath.toString());

        // Add the issue reported with the information
        issues.add(issue);
    }

    List<Issue> getIssues() {
        return issues;
    }
}

