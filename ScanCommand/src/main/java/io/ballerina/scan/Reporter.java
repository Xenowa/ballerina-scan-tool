package io.ballerina.scan;

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.scan.utilities.Rule;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static io.ballerina.scan.InbuiltRules.CUSTOM_RULES;
import static io.ballerina.scan.InbuiltRules.INBUILT_RULES;
import static io.ballerina.scan.utilities.ScanToolConstants.CUSTOM_CHECK_VIOLATION;
import static io.ballerina.scan.utilities.ScanToolConstants.SONARQUBE_RESERVED_RULES;

public class Reporter {

    private final ArrayList<Issue> issues;

    // TODO: To be made non-static once property bag is introduced by project API
    private static final AtomicInteger lastRuleIndex = new AtomicInteger(
            SONARQUBE_RESERVED_RULES + INBUILT_RULES.size());

    Reporter(ArrayList<Issue> issues) {

        this.issues = issues;
    }

    // TODO: To be removed once property bag is introduced by project API
    ArrayList<Issue> getIssues() {

        ArrayList<Issue> existingIssues = new ArrayList<>(issues);
        issues.clear();
        return existingIssues;
    }

    public synchronized void reportIssue(int startLine,
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

    private String generateCustomRuleID(String customRuleMessage) {
        // Check if the custom rule has a valid message
        if (customRuleMessage.isEmpty()) {
            return null;
        }

        // Check if the custom rule message already exists and if so return the rule assigned to the message
        if (CUSTOM_RULES.containsKey(customRuleMessage)) {
            return CUSTOM_RULES.get(customRuleMessage).getRuleID();
        }

        // Increment the last rule index
        lastRuleIndex.getAndIncrement();

        // Create custom rule ID
        String customRuleID = "S" + lastRuleIndex.get();

        // Put the message mapped with the custom rule ID
        CUSTOM_RULES.put(customRuleMessage, new Rule(customRuleID, customRuleMessage, true));

        // Put the new rule ID to the inbuilt rules array
        INBUILT_RULES.put(customRuleID, new Rule(customRuleID, customRuleMessage, true));

        // return the ruleID
        return customRuleID;
    }
}
