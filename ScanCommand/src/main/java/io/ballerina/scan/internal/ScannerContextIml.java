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

import io.ballerina.scan.Issue;
import io.ballerina.scan.Reporter;
import io.ballerina.scan.ScannerContext;

import java.util.List;

public class ScannerContextIml implements ScannerContext {

    private final ReporterIml reporter;

    ScannerContextIml(List<Issue> issues) {
        this.reporter = new ReporterIml(issues);
    }

    @Override
    public synchronized Reporter getReporter() {
        return reporter;
    }

    // TODO: Internal method To be removed ones project API fix is in effect
    synchronized List<Issue> getAllIssues() {
        return reporter.getIssues();
    }

    // NOTES
    //  - Rules to filter should be passed through the compiler plugin context through scanner context
    //  - It's plugin developers responsibility to implement visitor checks in a way they can be enabled/disabled by
    //  the passed rules
    //  - There is still the problem of identifying which plugins reported which issues to be solved
}
