package org.wso2.ballerina.internal.miscellaneous;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.Issue;

import java.util.ArrayList;

// Custom rules extension point for bal scan tool
public abstract class DeprecatedCustomScanner {
    private static ArrayList<Issue> issues = new ArrayList<>();
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String CUSTOM_RULE_ID = "CUSTOM_RULE_ID";

    // Method is utilized to retrieve a copy of the issues array
    public static ArrayList<Issue> getIssues() {
        return issues;
    }

    public static void setIssue(Issue issue) {
        issues.add(issue);
    }

    /**
     * This method should be used by custom tool plugins to implement new rules
     * To create custom rules the {@link io.ballerina.compiler.syntax.tree.NodeVisitor} can be extended
     */
    public abstract void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel);
}
