package org.wso2.ballerina;

import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import org.wso2.ballerina.internal.utilities.ScanToolConstants;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;
import org.wso2.ballerinalang.compiler.diagnostic.properties.BStringProperty;

import java.util.ArrayList;
import java.util.List;

// Custom rules extension point for bal scan tool
public abstract class CustomScanner {
    private final ArrayList<Issue> issues = new ArrayList<>(); // checking if static solves concurrency issues

    public void setIssue(Issue issue) {
        issues.add(issue);
    }

    public ArrayList<Issue> getIssues() {
        return new ArrayList<>(issues);
    }

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
                    ScanToolConstants.CUSTOM_CHECK_VIOLATION,
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
}