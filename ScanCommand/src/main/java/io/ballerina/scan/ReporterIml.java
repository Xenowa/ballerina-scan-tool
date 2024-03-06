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

public class ReporterIml implements Reporter {

    private final ArrayList<Issue> issues;
    private final ArrayList<String> definedRules;

    // Initial approach
//    ReporterIml(ArrayList<Issue> issues) {
//        this.issues = issues;
//    }
//    @Override
//    public synchronized void reportIssue(Issue issue) {
//        issues.add(issue);
//    }

    // TODO: approach to be tested
    ReporterIml(ArrayList<Issue> issues, ArrayList<String> definedRules) {
        this.issues = issues;
        this.definedRules = definedRules;
    }

    @Override
    public synchronized void reportIssue(Issue issue) {
        // Only report if the provided rule is present/activated
        if (definedRules.contains(issue.getRuleID())) {
            issues.add(issue);
        }
    }

    // TODO: Internal method to be removed once property bag is introduced by project API
    ArrayList<Issue> getIssues() {
        ArrayList<Issue> existingIssues = new ArrayList<>(issues);
        issues.clear();
        return existingIssues;
    }
}

