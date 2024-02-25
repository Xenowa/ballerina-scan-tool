package io.ballerina.scan;

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;

import java.nio.file.Path;
import java.util.ArrayList;

import static io.ballerina.scan.InbuiltRules.INBUILT_RULES;

public class InternalReporter {

    private final ArrayList<Issue> issues;

    InternalReporter(ArrayList<Issue> issues) {

        this.issues = issues;
    }

    void reportIssue(int startLine,
                     int startLineOffset,
                     int endLine,
                     int endLineOffset,
                     String ruleID,
                     String message,
                     String issueType,
                     String type,
                     Document reportedDocument,
                     Module reportedModule,
                     Project reportedProject) {

        String moduleName = reportedModule.moduleName().toString();
        String documentName = reportedDocument.name();
        Path issuesFilePath = reportedProject.documentPath(reportedDocument.documentId()).orElse(null);

        if (issuesFilePath != null && INBUILT_RULES.containsKey(ruleID)) {
            Issue issue = new Issue(startLine,
                    startLineOffset,
                    endLine,
                    endLineOffset,
                    ruleID,
                    message,
                    issueType,
                    type,
                    moduleName + "/" + documentName,
                    issuesFilePath.toString());

            issues.add(issue);
        }
    }

    void addExternalIssues(ArrayList<Issue> externalIssues) {

        issues.addAll(externalIssues);
    }
}
