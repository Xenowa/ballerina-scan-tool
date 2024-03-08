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

import java.util.ArrayList;

import static io.ballerina.scan.InbuiltRules.INBUILT_RULES;
import static io.ballerina.scan.utilities.ScanToolConstants.CORE_ISSUE;

public class InternalReporter {

    private final ArrayList<Issue> issues;
    private final String reportedSource;

    InternalReporter(ArrayList<Issue> issues, String reportedSource) {

        this.issues = issues;
        this.reportedSource = reportedSource;
    }

    void reportIssue(Issue issue) {
        if (INBUILT_RULES.containsKey(issue.getRuleID()) && INBUILT_RULES.get(issue.getRuleID()).ruleIsActivated()) {
            // Cast the issue to its implementation format to perform operations
            IssueIml castedIssue = (IssueIml) issue;

            // Set the issue reported source
            castedIssue.setReportedSource(reportedSource);

            // Set issue type
            castedIssue.setIssueType(CORE_ISSUE);

            issues.add(castedIssue);
        }
    }

    void addExternalIssues(ArrayList<Issue> externalIssues) {

        issues.addAll(externalIssues);
    }

    public String getReportedSource() {
        return reportedSource;
    }
}
