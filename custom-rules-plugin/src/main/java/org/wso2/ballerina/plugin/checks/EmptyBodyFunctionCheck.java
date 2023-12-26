package org.wso2.ballerina.plugin.checks;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.ReportIssue;

import java.util.ArrayList;

import static org.wso2.ballerina.CustomScanner.CUSTOM_CHECK_VIOLATION;
import static org.wso2.ballerina.CustomScanner.CUSTOM_RULE_ID;

public class EmptyBodyFunctionCheck extends ReportIssue {
    private final FunctionBodyBlockNode functionBodyBlockNode;

    public EmptyBodyFunctionCheck(FunctionBodyBlockNode functionBodyBlockNode, ArrayList<Issue> externalIssues) {
        super(externalIssues);
        this.functionBodyBlockNode = functionBodyBlockNode;
    }

    protected void triggerCheck() {
        // retrieve the function signature from each function

        // if function body is empty then report
        if (functionBodyBlockNode.statements().isEmpty()) {
            // Create a new issue
            Issue newExternalIssue = new Issue(functionBodyBlockNode.lineRange().startLine().line(),
                    functionBodyBlockNode.lineRange().startLine().offset(),
                    functionBodyBlockNode.lineRange().endLine().line(),
                    functionBodyBlockNode.lineRange().endLine().offset(),
                    CUSTOM_RULE_ID,
                    "Add a nested comment explaining why" +
                            " this function is empty or complete the implementation.",
                    CUSTOM_CHECK_VIOLATION,
                    functionBodyBlockNode.syntaxTree().filePath()
            );

            // Report the issue
            reportIssue(newExternalIssue);
        }
    }
}
