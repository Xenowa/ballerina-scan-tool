package org.wso2.ballerina.plugin.checks;

import com.google.gson.JsonArray;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import org.wso2.ballerina.ReportJsonIssue;

public class EmptyBodyFunctionCheck extends ReportJsonIssue {
    private final FunctionBodyBlockNode functionBodyBlockNode;

    public EmptyBodyFunctionCheck(FunctionBodyBlockNode functionBodyBlockNode, JsonArray externalIssues) {
        super(externalIssues);
        this.functionBodyBlockNode = functionBodyBlockNode;
    }

    // =========
    // Old Logic
    // =========
    protected void triggerCheck() {
        // retrieve the function signature from each function

        // if function body is empty then report
        if (functionBodyBlockNode.statements().isEmpty()) {
            // Reporting logic goes here
            reportIssue(
                    functionBodyBlockNode.lineRange(),
                    "Add a nested comment explaining why" +
                            " this function is empty or complete the implementation."
            );
        }
    }

    // TODO:
    //  =========
    //  New Logic (Simulating how reporting should work)
    //  =========
    //  - This logic should be moved to the actual Bal Scan tool and implemented via the compiler plugin
    //  - Currently it's only specifically available for this plugin
    //  - This currently only simulates what's the possible approach
    protected void triggerCheck(SyntaxNodeAnalysisContext context) {
        // if function body is empty then report
        if (functionBodyBlockNode.statements().isEmpty()) {
            // Reporting logic goes here
            reportIssue(
                    context,
                    functionBodyBlockNode,
                    "Add a nested comment explaining why" +
                            " this function is empty or complete the implementation."
            );
        }
    }

    public void reportIssue(SyntaxNodeAnalysisContext context, Node syntaxNode, String message) {
        Location issueLocation = syntaxNode.location(); // startLine, startLineOffset, endLine, endLineOffset
        DiagnosticInfo issueInfo = new DiagnosticInfo(
                "CUSTOM_CHECK_VIOLATION", // issueType
                message, // message
                DiagnosticSeverity.WARNING);

        Diagnostic issue = DiagnosticFactory.createDiagnostic(issueInfo, issueLocation, syntaxNode);
        context.reportDiagnostic(issue);
    }
}
