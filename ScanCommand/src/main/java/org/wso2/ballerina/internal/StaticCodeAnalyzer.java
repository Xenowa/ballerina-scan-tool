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

import static org.wso2.ballerina.internal.utilities.ScanToolConstants.CHECK_VIOLATION;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.CODE_SMELL;

public class StaticCodeAnalyzer extends NodeVisitor {
    ScannerContext scannerContext;

    // Initialize the static code analyzer
    private static Node mainNode;

    public StaticCodeAnalyzer(SyntaxTree syntaxTree) {
        mainNode = syntaxTree.rootNode();
    }

    public void initialize(ScannerContext scannerContext) {
        this.scannerContext = scannerContext;
        // Go with the following approach similar to CodeAnalyzer
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
            scannerContext.getReporter().reportIssue(
                    functionSignatureNode.lineRange().startLine().line(),
                    functionSignatureNode.lineRange().startLine().offset(),
                    functionSignatureNode.lineRange().endLine().line(),
                    functionSignatureNode.lineRange().endLine().offset(),
                    "S107",
                    "This function has "
                            + parameterCount
                            + " parameters, which is greater than the 7 authorized.",
                    CHECK_VIOLATION,
                    CODE_SMELL,
                    scannerContext.getCurrentDocument(),
                    scannerContext.getCurrentModule(),
                    scannerContext.getCurrentProject()
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
            scannerContext.getReporter().reportIssue(
                    functionBodyBlockNode.lineRange().startLine().line(),
                    functionBodyBlockNode.lineRange().startLine().offset(),
                    functionBodyBlockNode.lineRange().endLine().line(),
                    functionBodyBlockNode.lineRange().endLine().offset(),
                    "S108",
                    "This function has "
                            + checkPanicCounter.get()
                            + " occurrences of checkpanic keyword. Please consider using the check keyword instead!",
                    CHECK_VIOLATION,
                    CODE_SMELL,
                    scannerContext.getCurrentDocument(),
                    scannerContext.getCurrentModule(),
                    scannerContext.getCurrentProject()
            );
        }

        this.visitSyntaxNode(functionBodyBlockNode);
    }
}
