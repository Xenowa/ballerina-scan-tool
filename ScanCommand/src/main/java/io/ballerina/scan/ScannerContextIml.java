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

import io.ballerina.projects.plugins.CompilerPluginContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScannerContextIml implements ScannerContext {

    private final ArrayList<String> definedRules;
    private final ReporterIml reporter;
    private final Map<CompilerPluginContext, ReporterIml> reporters = new ConcurrentHashMap<>();

    // TODO: to be removed if no concurrent compiler plugin engagements occur
    //  =============================
    //  Method 1: Parallel reporting
    //  =============================
    //  - getReporter() accepts compiler plugin context parameter
    //  - There are multiple instances of the reporter depending on the number of compiler plugins
    ScannerContextIml(ArrayList<String> definedRules) {
        this.definedRules = definedRules;
        this.reporter = null;
    }

    @Override
    public synchronized Reporter getReporter2(CompilerPluginContext compilerPluginContext) {
        // Return existing reporter
        if (reporters.containsKey(compilerPluginContext)) {
            return reporters.get(compilerPluginContext);
        }

        // create a new reporter and add to hashmap
        ArrayList<Issue> externalIssues = new ArrayList<>();
        ReporterIml newReporter = new ReporterIml(externalIssues, definedRules);
        reporters.put(compilerPluginContext, newReporter);
        return newReporter;
    }

    // TODO: to be removed if compiler plugins engage concurrently
    //  ================================
    //  Method 2: synchronized reporting (Ideal Approach)
    //  ================================
    //  - getReporter() accepts no parameters
    //  - There is only 1 instance of the reporter
    ScannerContextIml(ArrayList<Issue> issues, ArrayList<String> definedRules) {
        this.definedRules = definedRules;
        this.reporter = new ReporterIml(issues, definedRules);
    }

    @Override
    public synchronized Reporter getReporter() {
        return reporter;
    }

    // TODO: NOTES
    //  - Rules to filter should be passed through the compiler plugin context through scanner context
    //  - It's plugin developers responsibility to implement visitor checks in a way they can be enabled/disabled by
    //  the passed rules
    //  - There is still the problem of identifying which plugins reported which issues to be solved

    // TODO: Internal method To be removed ones project API fix is in effect
    synchronized ReporterIml getReporterIml() {
        return reporter;
    }

    synchronized ArrayList<Issue> getAllIssues() {
        ArrayList<Issue> allIssues = new ArrayList<>();
        reporters.values().forEach(reporterIml -> {
            allIssues.addAll(reporterIml.getIssues());
        });
        
        return allIssues;
    }
}
