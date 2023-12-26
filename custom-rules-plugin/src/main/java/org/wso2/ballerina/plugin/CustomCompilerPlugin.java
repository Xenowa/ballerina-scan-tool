package org.wso2.ballerina.plugin;

import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import org.wso2.ballerina.CustomScanner;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.plugin.checks.CustomChecks;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import org.wso2.ballerinalang.compiler.diagnostic.properties.NonCatProperty;

import java.util.ArrayList;
import java.util.List;

public class CustomCompilerPlugin extends CustomScanner implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        System.out.println("Custom rules compiler plugin is in action =)");

        // Performing custom rule analysis
        CustomChecks customChecks = new CustomChecks(context.syntaxTree());
        ArrayList<Issue> externalIssues = getIssues();
        customChecks.initialize(externalIssues);

        // Set all issues to the issues array
        externalIssues.forEach(issue -> {
            setIssue(issue);
        });

        // Report all issues
        reportIssues(context);
    }
}
