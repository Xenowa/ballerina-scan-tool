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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.internal.parser.tree.STToken;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;

import java.util.List;
import java.util.Objects;

import static io.ballerina.scan.InbuiltRules.INBUILT_RULES;

public class StaticCodeAnalyzer extends NodeVisitor {

    private final Document currentDocument;
    private final SyntaxTree syntaxTree;
    private final SemanticModel semanticModel;
    private final InternalScannerContext scannerContext;

    public StaticCodeAnalyzer(Document currentDocument,
                              SyntaxTree syntaxTree,
                              SemanticModel semanticModel,
                              InternalScannerContext scannerContext) {
        this.currentDocument = currentDocument;
        this.syntaxTree = syntaxTree;
        this.semanticModel = semanticModel;
        this.scannerContext = scannerContext;
    }

    public void initialize() {
        // Go with the following approach similar to CodeAnalyzer
        this.visit((ModulePartNode) syntaxTree.rootNode());

        // Other method to start the visit
        // mainNode.accept(this);
    }

    @Override
    public void visit(FunctionSignatureNode functionSignatureNode) {
        // Perform the analysis
        int parameterCount = functionSignatureNode.parameters().size();
        int allowedParametersLimit = 7;
        if (parameterCount > allowedParametersLimit) {
            // Report issue
            scannerContext.getReporter().reportIssue(currentDocument,
                    functionSignatureNode.location(),
                    Objects.requireNonNull(INBUILT_RULES.stream()
                            .filter(rule -> rule.numericId() == 107)
                            .findFirst().orElse(null)));
        }

        // Continue visiting other nodes of the syntax tree
        this.visitSyntaxNode(functionSignatureNode);
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        functionBodyBlockNode.statements().forEach(statementNode -> {
            statementNode.children().forEach(childPair -> {
                List<STToken> tokens = childPair.internalNode().tokens();
                tokens.forEach(token -> {
                    if (token.kind.equals(SyntaxKind.CHECKPANIC_KEYWORD)) {
                        // Report the issue
                        scannerContext.getReporter().reportIssue(currentDocument,
                                childPair.location(),
                                Objects.requireNonNull(INBUILT_RULES.stream()
                                        .filter(rule -> rule.numericId() == 108)
                                        .findFirst().orElse(null)));
                    }
                });
            });
        });

        this.visitSyntaxNode(functionBodyBlockNode);
    }
}
