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
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.StaticCodeAnalyzerPlugin;

import java.util.ArrayList;

public class CustomStaticCodeAnalyzer extends StaticCodeAnalyzerPlugin {

    @Override
    public ArrayList<String> definedRules() {
        ArrayList<String> customRules = new ArrayList<>();
        customRules.add("S109");
        customRules.add("S110");
        customRules.add("S111");
        customRules.add("S112");
        customRules.add("S113");
        return customRules;
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
                            Reporter reporter = scannerContext.getReporter2(compilerPluginContext);
                            
                            reporter.reportIssue(new IssueIml(
                                    functionBodyBlockNode.lineRange().startLine().line(),
                                    functionBodyBlockNode.lineRange().startLine().offset(),
                                    functionBodyBlockNode.lineRange().endLine().line(),
                                    functionBodyBlockNode.lineRange().endLine().offset(),
                                    "S109",
                                    "Add a nested comment explaining why" +
                                            " this function is empty or complete the implementation.",
                                    "CODE_SMELL",
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
