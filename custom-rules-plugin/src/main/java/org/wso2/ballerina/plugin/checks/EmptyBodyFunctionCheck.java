package org.wso2.ballerina.plugin.checks;

import com.google.gson.JsonArray;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import org.wso2.ballerina.ReportJsonIssue;

public class EmptyBodyFunctionCheck extends ReportJsonIssue {
    private final FunctionBodyBlockNode functionBodyBlockNode;
    String ruleID = "S109";

    public EmptyBodyFunctionCheck(FunctionBodyBlockNode functionBodyBlockNode, JsonArray externalIssues) {
        super(externalIssues);
        this.functionBodyBlockNode = functionBodyBlockNode;
    }

    protected void triggerCheck() {
        // retrieve the function signature from each function

        // if function body is empty then report
        if (functionBodyBlockNode.statements().isEmpty()) {
            // Reporting logic goes here
            reportIssue(
                    functionBodyBlockNode.lineRange(),
                    ruleID,
                    "Add a nested comment explaining why" +
                            " this function is empty or complete the implementation."
            );
        }
    }
}
