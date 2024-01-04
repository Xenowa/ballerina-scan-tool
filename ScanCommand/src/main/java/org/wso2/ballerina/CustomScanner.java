package org.wso2.ballerina;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import org.wso2.ballerina.internal.ReportLocalIssue;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import org.wso2.ballerinalang.compiler.diagnostic.properties.BStringProperty;

import java.util.ArrayList;
import java.util.List;

// Custom rules extension point for bal scan tool
public abstract class CustomScanner {
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String CUSTOM_RULE_ID = "CUSTOM_RULE_ID";
    private final ArrayList<Issue> issues = new ArrayList<>(); // checking if static solves concurrency issues

    public void setIssue(Issue issue) {
        issues.add(issue);
    }

    public ArrayList<Issue> getIssues() {
        return new ArrayList<>(issues);
    }

    /**
     * This method should be used by custom tool plugins to report issues
     * To create custom rules the {@link io.ballerina.compiler.syntax.tree.NodeVisitor} can be extended
     */

    // Method 1: Reporting issues as diagnostics [Current Implementation]
    // - This method gets called from the compiler plugin itself during package compilation
    // - This method is NOT used by serviceloaders in the Ballerina scan tool
    public void reportIssues(CompilationAnalysisContext context) {
        getIssues().forEach(issue -> {
            // Retrieve the location of the issue
            Location issueLocation = new BLangDiagnosticLocation(issue.getReportedFilePath(),
                    issue.getStartLine(),
                    issue.getEndLine(),
                    issue.getStartLineOffset(),
                    issue.getEndLineOffset());

            // Create Diagnostics information
            DiagnosticInfo issueInfo = new DiagnosticInfo(
                    CUSTOM_CHECK_VIOLATION,
                    issue.getMessage(),
                    DiagnosticSeverity.INTERNAL);

            // Add additional diagnostics properties relevant to the issue
            List<DiagnosticProperty<?>> diagnosticProperties = new ArrayList<>();

            // Add the issue type as a property
            DiagnosticProperty<String> ruleID = new BStringProperty(issue.getRuleID());
            diagnosticProperties.add(ruleID);

            // Add the filepath as a property
            DiagnosticProperty<String> reportedFilePath = new BStringProperty(issue.getReportedFilePath());
            diagnosticProperties.add(reportedFilePath);

            // Create a new diagnostic
            Diagnostic diagnosticIssue = DiagnosticFactory.createDiagnostic(issueInfo,
                    issueLocation,
                    diagnosticProperties, issue);

            // Report the diagnostic
            context.reportDiagnostic(diagnosticIssue);
        });
    }

    // Method 2: sending an issue reporter and retrieving issues through that
    // - In this case how to connect CompilationAnalysisContext?
    // - This method is called from serviceloaders in the Ballerina scan tool
    public void report(ReportLocalIssue issueReporter) {
        issueReporter.reportExternalIssues(issues);
        System.out.println("Using report method with issue reporter!");
    }

    // Method 3: Passing arguments to perform analysis and report
    // - The syntax tree and semantic model and reporter are passed from the Ballerina Scan Tool
    // - In this case the CompilationAnalysisContext is not required
    // - Therefore the perform(CompilationAnalysisContext context){} method of compiler plugin is not used
    // - This method is called from serviceloaders in the Ballerina scan tool
    public void report(ReportLocalIssue issueReporter, SyntaxTree syntaxTree, SemanticModel semanticModel) {
        issueReporter.reportExternalIssues(issues);
        System.out.println("Using Ballerina Scan Tool for analysis and reporting!");
    }
}