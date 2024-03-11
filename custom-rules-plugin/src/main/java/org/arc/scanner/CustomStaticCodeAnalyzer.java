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

package org.arc.scanner;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CompilerPluginContext;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.IssueIml;
import io.ballerina.scan.Reporter;
import io.ballerina.scan.Rule;
import io.ballerina.scan.RuleSeverity;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.StaticCodeAnalyzerPlugin;
import io.ballerina.scan.utilities.RuleMap;

public class CustomStaticCodeAnalyzer extends StaticCodeAnalyzerPlugin {

    private final RuleMap customRules;

    public CustomStaticCodeAnalyzer() {
        RuleMap customRules = new RuleMap();
        customRules.put("S109", new Rule("S109", "Add a nested comment explaining why" +
                " this function is empty or complete the implementation.", RuleSeverity.CODE_SMELL, true));
        customRules.put("S110", new Rule("S110", "rule 110",
                RuleSeverity.CODE_SMELL, true));
        customRules.put("S111", new Rule("S111", "rule 111",
                RuleSeverity.CODE_SMELL, true));
        customRules.put("S112", new Rule("S112", "rule 112",
                RuleSeverity.CODE_SMELL, true));
        this.customRules = customRules;
    }

    @Override
    public RuleMap definedRules() {
        return customRules.copy();
    }

    @Override
    public void init(CompilerPluginContext compilerPluginContext) {
        compilerPluginContext.addCodeAnalyzer(new CodeAnalyzer() {
            @Override
            public void init(CodeAnalysisContext codeAnalysisContext) {
                codeAnalysisContext.addSyntaxNodeAnalysisTask(new AnalysisTask<SyntaxNodeAnalysisContext>() {
                    @Override
                    public void perform(SyntaxNodeAnalysisContext context) {
                        Module module = context.currentPackage().module(context.moduleId());
                        Document document = module.document(context.documentId());
                        Project project = module.project();

                        FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) context.node();

                        // CUSTOM RULE: if function body is empty then report issue
                        if (functionBodyBlockNode.statements().isEmpty()) {
                            ScannerContext scannerContext = getScannerContext(compilerPluginContext);
                            Reporter reporter = scannerContext.getReporter(compilerPluginContext);

                            reporter.reportIssue(new IssueIml(
                                    functionBodyBlockNode.lineRange().startLine().line(),
                                    functionBodyBlockNode.lineRange().startLine().offset(),
                                    functionBodyBlockNode.lineRange().endLine().line(),
                                    functionBodyBlockNode.lineRange().endLine().offset(),
                                    customRules.get("S109"),
                                    document,
                                    module,
                                    project)
                            );

                            complete();
                        }
                    }
                }, SyntaxKind.FUNCTION_BODY_BLOCK);
            }
        });
    }
}
