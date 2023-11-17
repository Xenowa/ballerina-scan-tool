package org.wso2.ballerina.internal.miscellaneous;

import io.ballerina.compiler.internal.parser.tree.STToken;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;

import static org.wso2.ballerina.internal.platforms.Platform.CHECK_VIOLATION;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CheckPanicCountCheck extends ReportJsonIssueOld {
    FunctionDefinitionNode startingNode;

    public CheckPanicCountCheck(FunctionDefinitionNode startingNode) {
        super("S108");
        this.startingNode = startingNode;
        activateRule();
    }

    public void triggerCheck() {
        // Retrieve the body of functions
        FunctionBodyNode functionBody = startingNode.functionBody();

        // Get the function body only
        FunctionBodyBlockNode functionBodyBlock = (FunctionBodyBlockNode) functionBody;

        // Retrieve statements of the function body
        NodeList<StatementNode> statements = functionBodyBlock.statements();

        AtomicInteger checkPanicCounter = new AtomicInteger(0);
        statements.forEach(statement -> {
            // Iterate through each statement
            statement.children().forEach(childPair -> {
                // Iterate through each child pair
                List<STToken> tokens = childPair.internalNode().tokens();
                tokens.forEach(token -> {
                    // If there are tokens with checkpanic keyword increment the checkpanic keyword counter
                    if (token.kind.equals(SyntaxKind.CHECKPANIC_KEYWORD)) {
                        checkPanicCounter.getAndIncrement();
                    }
                });
            });
        });

        if (checkPanicCounter.get() > 0) {
            reportIssue(
                    CHECK_VIOLATION,
                    functionBody.lineRange(),
                    "This function has "
                            + checkPanicCounter.get()
                            + " occurrences of checkpanic keyword. Please consider using the check keyword instead!"
            );
        }
    }

}
