package org.wso2.ballerina;

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import java.util.ArrayList;

// Custom rules extension point for bal scan tool
public interface ExternalRules {
    ArrayList<String> CUSTOM_RULES = new ArrayList<>();
    JsonArray externalIssues = new JsonArray();

    /**
     * This method should be used to introduce custom ruleID's for the rules
     * in the {@link org.wso2.ballerina.ExternalRules#CUSTOM_RULES} list
     */
    void setCustomRules();

    // This method is used by the bal scan tool internally to retrieve the custom rules
    ArrayList<String> getCustomRules();

    // This method is used by the bal scan tool internally to retrieve the custom issues
    JsonArray getExternalIssues();

    /**
     * This method should be used by custom tool plugins to implement new rules
     * To create custom rules the {@link io.ballerina.compiler.syntax.tree.NodeVisitor} can be extended
     */
    void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel);
}
