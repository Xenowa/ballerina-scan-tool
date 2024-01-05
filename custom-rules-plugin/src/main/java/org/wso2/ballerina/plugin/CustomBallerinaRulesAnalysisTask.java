package org.wso2.ballerina.plugin;

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import org.wso2.ballerina.CustomScanner;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.plugin.checks.CustomChecks;

import java.nio.file.Path;
import java.util.ArrayList;

public class CustomBallerinaRulesAnalysisTask extends CustomScanner implements AnalysisTask<CompilationAnalysisContext> {
    // ==================================
    // reporter called from bal scan tool
    // ==================================
    // TODO:
    //  IssueReporter issueReporter;
    //  initialize(IssueReporter issueReporter){
    //      this.issueReporter = issueReporter;
    //  }
    // - It's possible to use the reporter as follows, however the perform method does not get called on compiler plugin
    // TODO:
    //  reporter(IssueReporter issueReporter, SyntaxTree st, SemanticModel sm){
    //      Issue customIssue = new Issue(...);
    //      issueReporter.reportIssue(customIssue);
    //  }

    // ================================
    // Compiler Plugin initiating point
    // ================================
    // - Method gets called only during package compilation
    // - So there is no way to pass the reporter to it
    // - As an alternative a custom diagnostic type can be introduced to report Issues from the reportDiagnostic()
    // TODO:
    //  Issue customIssue = new Issue(...);
    //  IssueDiagnostic newIssueDiagnostic = new IssueDiagnostic(..., customIssue)
    //  context.reportDiagnostic(newIssueDiagnostic);
    // the tool without needing a reporter method from the bal scan tool
    // Implementation of analysis task to be run during the compilation
    @Override
    public void perform(CompilationAnalysisContext context) {
        // This is the correct approach (There is only a single run)
        System.out.println("Custom rules compiler plugin is in action =)");

        // Array to hold all issues
        ArrayList<Issue> externalIssues = getIssues();

        context.currentPackage().moduleIds().forEach(moduleId -> {
            Module module = context.currentPackage().module(moduleId);

            // Iterate through each Ballerina test file
            module.testDocumentIds().forEach(testDocumentID -> {
                Document testDocument = module.document(testDocumentID);

                // Retrieve path of the document being analyzed
                Path documentPath = module.project().documentPath(testDocumentID).orElse(null);
                if (documentPath != null) {
                    CustomChecks customChecks = new CustomChecks(testDocument.syntaxTree(),
                            documentPath.toAbsolutePath().toString());
                    customChecks.initialize(externalIssues);
                }
            });

            // Iterate through each Ballerina file
            module.documentIds().forEach(documentId -> {
                Document document = module.document(documentId);

                // Retrieve path of the document being analyzed
                Path documentPath = module.project().documentPath(documentId).orElse(null);
                if (documentPath != null) {
                    CustomChecks customChecks = new CustomChecks(document.syntaxTree(),
                            documentPath.toAbsolutePath().toString());
                    customChecks.initialize(externalIssues);
                }
            });
        });

        // Set all issues to the issues array
        externalIssues.forEach(issue -> {
            setIssue(issue);
        });

        // Report all issues
        reportIssues(context);
    }
}
