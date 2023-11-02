package org.wso2.ballerina.checks;

import io.ballerina.compiler.internal.parser.tree.STToken;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.wso2.ballerina.platforms.SonarQube.CHECK_VIOLATION;

public class FunctionChecks extends ReportJsonIssue{
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
                // ======================
                // TooManyParametersCheck
                // ======================
                // retrieve the function signature from each function
                FunctionSignatureNode functionSignature = (FunctionSignatureNode) functionDefinitionNode.functionSignature();

                // get the number of parameters inside of the function signature as a list (commas included)
                NodeList<ParameterNode> parameters = functionSignature.parameters();

                // Retrieve all parameters excluding the commas
                // int parameterCount = parameters.toSourceCode().split(",").length;
                int parameterCount = parameters.size();

                // Apply the check for too Many Parameters
                if (parameterCount > 7) {
                    reportIssue(
                            CHECK_VIOLATION,
                            functionDefinitionNode.lineRange(),
                            "S107",
                            "This function has " + parameterCount + " parameters, which is greater than the 7 authorized."
                    );
                }


                // ======================
                // CheckPanicKeywordCheck
                // ======================
                // Retrieve the body of functions
                FunctionBodyNode functionBody = functionDefinitionNode.functionBody();

                // Get the function body only
                FunctionBodyBlockNode functionBodyBlock = (FunctionBodyBlockNode) functionBody;

                // Retrieve statements of the function body
                NodeList<StatementNode> statements = functionBodyBlock.statements();

                AtomicInteger checkPanicCounter = new AtomicInteger(0);
                    statements.forEach(statement ->{
                        // Iterate through each statement
                        statement.children().forEach(childPair ->{
                            // Iterate through each child pair
                            List<STToken> tokens = childPair.internalNode().tokens();
                            tokens.forEach(token ->{
                                // If there are tokens with checkpanic keyword increment the checkpanic keyword counter
                                if(token.kind.equals(SyntaxKind.CHECKPANIC_KEYWORD)){
                                    checkPanicCounter.getAndIncrement();
                                }
                            });
                        });
                    });

                if(checkPanicCounter.get() > 0){
                    reportIssue(
                            CHECK_VIOLATION,
                            functionBody.lineRange(),
                            "S108",
                            "This function has " + checkPanicCounter.get() + " occurrences of checkpanic keyword. Please consider using the check keyword instead!"
                    );
                }

                // ======================
                // SomeOtherFunctionCheck
                // ======================
            }
        });
    }
}

// NOTE:
// There is a compromise related to readability when making the visitor more performant  as in this file
// For some reason the line ranges reported by the parsed elements are not accurate