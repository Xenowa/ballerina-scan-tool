package org.wso2.ballerina.checks.functionChecks;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

public class FunctionChecks{
    // TODO: The whole approach of getting and using the Syntax Tree should be different
    // Current implementation uses the internal syntax tree which should be avoided and replaced by the usage of the
    // external syntax tree

    // Initializing the function checks
    Node mainNode;
    public FunctionChecks(SyntaxTree syntaxTree){
        this.mainNode = syntaxTree.rootNode();
    }

    public void initialize(){
        // Entry point to all visitors
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