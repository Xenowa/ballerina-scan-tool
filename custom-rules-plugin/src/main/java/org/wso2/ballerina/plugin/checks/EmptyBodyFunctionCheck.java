package org.wso2.ballerina.plugin.checks;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.ReportExternalIssue;

import java.util.ArrayList;

import static org.wso2.ballerina.CustomScanner.CUSTOM_CHECK_VIOLATION;
import static org.wso2.ballerina.CustomScanner.CUSTOM_RULE_ID;

public class EmptyBodyFunctionCheck extends ReportExternalIssue {
    private final FunctionBodyBlockNode functionBodyBlockNode;

    public EmptyBodyFunctionCheck(FunctionBodyBlockNode functionBodyBlockNode,
                                  ArrayList<Issue> externalIssues,
                                  String issuesFilePath) {
        super(externalIssues, issuesFilePath);
        this.functionBodyBlockNode = functionBodyBlockNode;
    }

    protected void triggerCheck() {
        // retrieve the function signature from each function

        // if function body is empty then report issue
        if (functionBodyBlockNode.statements().isEmpty()) {
            reportIssue(functionBodyBlockNode.lineRange().startLine().line(),
                    functionBodyBlockNode.lineRange().startLine().offset(),
                    functionBodyBlockNode.lineRange().endLine().line(),
                    functionBodyBlockNode.lineRange().endLine().offset(),
                    CUSTOM_RULE_ID,
                    "Add a nested comment explaining why" +
                            " this function is empty or complete the implementation.",
                    CUSTOM_CHECK_VIOLATION);
        }
    }
}
