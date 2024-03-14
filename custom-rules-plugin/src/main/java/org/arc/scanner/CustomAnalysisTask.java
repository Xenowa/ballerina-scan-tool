/*
 *
 *  * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *  *
 *  * WSO2 LLC. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.arc.scanner;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.IssueIml;
import io.ballerina.scan.Reporter;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.utilities.RuleMap;

public class CustomAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private final CustomStaticCodeAnalyzer customStaticCodeAnalyzer;
    private final ScannerContext scannerContext;
    private final RuleMap customRules;

    public CustomAnalysisTask(CustomStaticCodeAnalyzer customStaticCodeAnalyzer, ScannerContext scannerContext,
                              RuleMap customRules) {
        this.customStaticCodeAnalyzer = customStaticCodeAnalyzer;
        this.scannerContext = scannerContext;
        this.customRules = customRules.copy();
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        Module module = context.currentPackage().module(context.moduleId());
        Document document = module.document(context.documentId());

        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) context.node();

        // CUSTOM RULE: if function body is empty then report issue
        if (functionBodyBlockNode.statements().isEmpty()) {
            Reporter reporter = scannerContext.getReporter();
            reporter.reportIssue(new IssueIml(functionBodyBlockNode.location(), customRules.get("S109"), document));

            customStaticCodeAnalyzer.complete();
        }
    }
}
