package org.wso2.ballerina.plugin.miscellaneous;

import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.wso2.ballerina.CustomScanner;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.plugin.checks.CustomChecks;

import java.util.ArrayList;

public class DeprecatedCustomCompilerPlugin extends CustomScanner implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        System.out.println("Custom rules compiler plugin is in action =)");

        // Performing custom rule analysis
        CustomChecks customChecks = new CustomChecks(context.syntaxTree(), "");
        ArrayList<Issue> externalIssues = getIssues();
        customChecks.initialize(externalIssues);

        // Set all issues to the issues array
        externalIssues.forEach(issue -> {
            setIssue(issue);
        });

        // Report all issues
        // reportIssues(context);
    }
}
