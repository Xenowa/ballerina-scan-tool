package org.wso2.ballerina.plugin.checks;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.ReportExternalIssue;
import org.wso2.ballerina.internal.ScanToolConstants;

import java.util.ArrayList;

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
                    ScanToolConstants.CUSTOM_RULE_ID,
                    "Add a nested comment explaining why" +
                            " this function is empty or complete the implementation.",
                    ScanToolConstants.CUSTOM_CHECK_VIOLATION);
        }
    }
}
