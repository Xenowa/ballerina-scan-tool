package org.wso2.ballerina.plugin.checks;

import com.google.gson.JsonArray;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

public class CustomChecks {
    // Initializing the function checks
    private final Node mainNode;

    public CustomChecks(SyntaxTree syntaxTree) {
        this.mainNode = syntaxTree.rootNode();
    }

    public void initialize(JsonArray externalIssues) {
        // Entry point to all function visitors
        mainNode.accept(new NodeVisitor() {
            @Override
            public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
                super.visit(functionBodyBlockNode);
                new EmptyBodyFunctionCheck(functionBodyBlockNode, externalIssues).triggerCheck();
            }
        });
    }

    // ==============================
    // For Testing NEW IMPLEMENTATION
    // ==============================
    public void initialize(SyntaxNodeAnalysisContext context, JsonArray externalIssues) {
        // Entry point to all function visitors
        mainNode.accept(new NodeVisitor() {
            @Override
            public void visit(FunctionBodyBlockNode functionBodyBlockNode) {
                super.visit(functionBodyBlockNode);
                new EmptyBodyFunctionCheck(functionBodyBlockNode, externalIssues).triggerCheck(context);
            }
        });
    }
}
