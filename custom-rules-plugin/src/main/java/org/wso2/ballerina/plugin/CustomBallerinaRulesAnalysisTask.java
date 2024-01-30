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
        externalIssues.forEach(this::setIssue);

        // Report all issues
        reportIssues(context);
    }
}
