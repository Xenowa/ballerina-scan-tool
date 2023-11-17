package org.wso2.ballerina.internal;

import io.ballerina.compiler.internal.parser.tree.STToken;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class StaticCodeAnalyzer extends NodeVisitor {
    ReportLocalIssue issueReporter = new ReportLocalIssue();

    // Initialize the static code analyzer
    private static Node mainNode;

    public StaticCodeAnalyzer(SyntaxTree syntaxTree) {
        mainNode = syntaxTree.rootNode();
    }

    public void initialize() {
        // Go with the following approach as in like the CodeAnalyzer
        this.visit((ModulePartNode) mainNode);

        // Other method to start the visit
        // mainNode.accept(this);
    }

    @Override
    public void visit(FunctionSignatureNode functionSignatureNode) {
        // Perform the analysis
        int parameterCount = functionSignatureNode.parameters().size();
        if (parameterCount > 7) {
            // Report issue
            issueReporter.reportIssue(
                    functionSignatureNode.lineRange(),
                    "S107",
                    "This function has "
                            + parameterCount
                            + " parameters, which is greater than the 7 authorized."
            );
        }

        // Continue visiting other nodes of the syntax tree
        this.visitSyntaxNode(functionSignatureNode);
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
        AtomicInteger checkPanicCounter = new AtomicInteger(0);

        functionBodyBlockNode.statements().forEach(statement -> {
            // Iterate through each statement
            statement.children().forEach(childPair -> {
                // Iterate through each child pair
                List<STToken> tokens = childPair.internalNode().tokens();
                tokens.forEach(token -> {
                    // If there are tokens with checkpanic keyword increment the counter
                    if (token.kind.equals(SyntaxKind.CHECKPANIC_KEYWORD)) {
                        checkPanicCounter.getAndIncrement();
                    }
                });
            });
        });

        if (checkPanicCounter.get() > 0) {
            // Report issue
            issueReporter.reportIssue(
                    functionBodyBlockNode.lineRange(),
                    "S108",
                    "This function has "
                            + checkPanicCounter.get()
                            + " occurrences of checkpanic keyword. Please consider using the check keyword instead!"
            );
        }

        this.visitSyntaxNode(functionBodyBlockNode);
    }
}
