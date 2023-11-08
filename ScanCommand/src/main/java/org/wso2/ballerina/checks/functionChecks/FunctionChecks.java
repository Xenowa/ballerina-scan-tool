package org.wso2.ballerina.checks.functionChecks;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

public class FunctionChecks{
    // Initializing the function checks
    Node mainNode;
    public FunctionChecks(SyntaxTree syntaxTree){
        this.mainNode = syntaxTree.rootNode();
    }

    public void initialize(){
        // Entry point to all function visitors
        mainNode.accept(new NodeVisitor(){
            @Override
            public void visit(FunctionDefinitionNode functionDefinitionNode) {
                super.visit(functionDefinitionNode);
                new TooManyParametersCheck().triggerCheck(functionDefinitionNode);
                new CheckPanicCountCheck().triggerCheck(functionDefinitionNode);
            }
        });
    }
}