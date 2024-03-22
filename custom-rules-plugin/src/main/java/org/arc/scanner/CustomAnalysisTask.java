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
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

import java.util.List;
import java.util.Objects;

public class CustomAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private final CustomStaticCodeAnalyzer customStaticCodeAnalyzer;
    private final Reporter reporter;
    private final List<Rule> customRules;

    public CustomAnalysisTask(ScannerContext scannerContext, List<Rule> customRules,
                              CustomStaticCodeAnalyzer customStaticCodeAnalyzer) {
        this.customStaticCodeAnalyzer = customStaticCodeAnalyzer;
        this.reporter = scannerContext.getReporter();
        this.customRules = customRules.stream().toList();
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {

        Module module = context.currentPackage().module(context.moduleId());
        Document document = module.document(context.documentId());

        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) context.node();

        // CUSTOM RULE: if function body is empty then report issue
        if (functionBodyBlockNode.statements().isEmpty()) {
            reporter.reportIssue(document, functionBodyBlockNode.location(),
                    Objects.requireNonNull(customRules.stream()
                            .filter(rule -> rule.numericId() == 109)
                            .findFirst().orElse(null)));

            customStaticCodeAnalyzer.complete();
        }
    }
}
