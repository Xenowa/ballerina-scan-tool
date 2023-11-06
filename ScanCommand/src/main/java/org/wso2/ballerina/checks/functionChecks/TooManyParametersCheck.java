package org.wso2.ballerina.checks.functionChecks;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import org.wso2.ballerina.checks.ReportJsonIssue;

import static org.wso2.ballerina.platforms.SonarQube.CHECK_VIOLATION;

public class TooManyParametersCheck extends ReportJsonIssue {
    public void triggerCheck(FunctionDefinitionNode functionDefinitionNode){
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
    }

}
