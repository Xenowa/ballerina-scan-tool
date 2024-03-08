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

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Optional;

import static io.ballerina.projects.util.ProjectConstants.BALLERINA_ORG;
import static io.ballerina.scan.utilities.ScanToolConstants.CORE_ISSUE;
import static io.ballerina.scan.utilities.ScanToolConstants.EXTERNAL_ISSUE;
import static io.ballerina.scan.utilities.ScanToolConstants.PATH_SEPARATOR;

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

    // Approach for incorporating rule filtering during reporting
    ReporterIml(ArrayList<Issue> issues, ArrayList<String> definedRules) {
        this.issues = issues;
        this.definedRules = definedRules;
    }

    @Override
    public synchronized void reportIssue(Issue issue) {
        // Only report if the provided rule is present/activated
        if (definedRules.contains(issue.getRuleID())) {

            // Getting org/name from URL through protection domain
            URL jarUrl = this.getClass()
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation();
            URI jarUri;
            try {
                jarUri = jarUrl.toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            Path jarPath = Paths.get(jarUri);
            String pluginName = Optional.of(jarPath)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .orElse("");

            String pluginOrg = Optional.of(jarPath)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getParent)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .orElse("");

            // Cast the issue to its implementation format to perform operations
            IssueIml castedIssue = (IssueIml) issue;
            castedIssue.setReportedSource(pluginOrg + PATH_SEPARATOR + pluginName);

            // Depending on the source set the issue type
            if (pluginOrg.equals(BALLERINA_ORG)) {
                castedIssue.setIssueType(CORE_ISSUE);
            } else {
                castedIssue.setIssueType(EXTERNAL_ISSUE);
            }

            // Add the cast issue reported with the information
            issues.add(castedIssue);
        }
    }

    // TODO: Internal method to be removed once property bag is introduced by project API
    ArrayList<Issue> getIssues() {
        ArrayList<Issue> existingIssues = new ArrayList<>(issues);
        issues.clear();
        return existingIssues;
    }
}

