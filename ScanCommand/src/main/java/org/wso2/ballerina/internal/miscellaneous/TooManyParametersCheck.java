package org.wso2.ballerina.internal.miscellaneous;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;

import static org.wso2.ballerina.internal.platforms.Local.CHECK_VIOLATION;

public class TooManyParametersCheck extends ReportJsonIssueOld {
    FunctionDefinitionNode startingNode;

    public TooManyParametersCheck(FunctionDefinitionNode startingNode) {
        super("S107");
        this.startingNode = startingNode;
        activateRule();
    }

    @Override
    protected void triggerCheck() {
        // retrieve the function signature from each function
        FunctionSignatureNode functionSignature = startingNode.functionSignature();

        // get the number of parameters inside of the function signature as a list (commas included)
        NodeList<ParameterNode> parameters = functionSignature.parameters();

        // Retrieve all parameters excluding the commas
        // int parameterCount = parameters.toSourceCode().split(",").length;
        int parameterCount = parameters.size();

        // Apply the check for too Many Parameters
        if (parameterCount > 7) {
            reportIssue(
                    CHECK_VIOLATION,
                    startingNode.lineRange(),
                    "This function has " + parameterCount + " parameters, which is greater than the 7 authorized."
            );
        }
    }
}
