package org.wso2.ballerina.plugin.checks;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.Issue;

import java.util.ArrayList;

public class CustomChecks {
    // Initializing the function checks
    private final Node mainNode;
    private String documentPath;

    public CustomChecks(SyntaxTree syntaxTree, String documentPath) {
        this.mainNode = syntaxTree.rootNode();
        this.documentPath = documentPath;
    }

    public void initialize(ArrayList<Issue> externalIssues) {
        // Entry point to all function visitors
        mainNode.accept(new NodeVisitor() {
            @Override
            public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
                super.visit(functionBodyBlockNode);
                new EmptyBodyFunctionCheck(functionBodyBlockNode, externalIssues, documentPath).triggerCheck();
            }
        });
    }
}
