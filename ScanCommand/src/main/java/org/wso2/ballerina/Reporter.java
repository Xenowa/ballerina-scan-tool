package org.wso2.ballerina;

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.internal.utilities.Rule;

import java.nio.file.Path;
import java.util.ArrayList;

import static org.wso2.ballerina.internal.InbuiltRules.CUSTOM_RULES;
import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.CUSTOM_CHECK_VIOLATION;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.SONARQUBE_RESERVED_RULES;

public class Reporter {
    private int lastRuleIndex = SONARQUBE_RESERVED_RULES + INBUILT_RULES.size();
    private final ArrayList<Issue> issues;

    public Reporter(ArrayList<Issue> issues) {
        this.issues = issues;
    }

    ArrayList<Issue> getIssues() {
        return issues;
    }

    void addExternalIssues(ArrayList<Issue> issues) {
        this.issues.addAll(issues);
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

        if (issuesFilePath != null) {
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

    public void reportExternalIssue(int startLine,
                                    int startLineOffset,
                                    int endLine,
                                    int endLineOffset,
                                    String message,
                                    String type,
                                    Document reportedDocument,
                                    Module reportedModule,
                                    Project reportedProject) {
        String moduleName = reportedModule.moduleName().toString();
        String documentName = reportedDocument.name();
        Path externalIssuesFilePath = reportedProject.documentPath(reportedDocument.documentId()).orElse(null);

        if (externalIssuesFilePath != null && message != null) {
            Issue issue = new Issue(startLine,
                    startLineOffset,
                    endLine,
                    endLineOffset,
                    generateCustomRuleID(message),
                    message,
                    CUSTOM_CHECK_VIOLATION,
                    type,
                    moduleName + "/" + documentName,
                    externalIssuesFilePath.toString());

            issues.add(issue);
        }
    }

    String generateCustomRuleID(String customRuleMessage) {
        // Check if the custom rule has a valid message
        if (customRuleMessage.isEmpty()) {
            return null;
        }

        // Check if the custom rule message already exists and if so return the rule assigned to the message
        if (CUSTOM_RULES.containsKey(customRuleMessage)) {
            return CUSTOM_RULES.get(customRuleMessage).getRuleID();
        }

        // Increment the last rule index
        lastRuleIndex++;

        // Create custom rule ID
        String customRuleID = "S" + lastRuleIndex;

        // Put the message mapped with the custom rule ID
        CUSTOM_RULES.put(customRuleMessage, new Rule(customRuleID, customRuleMessage, true));

        // Put the new rule ID to the inbuilt rules array
        INBUILT_RULES.put(customRuleID, new Rule(customRuleID, customRuleMessage, true));

        // return the ruleID
        return customRuleID;
    }
}