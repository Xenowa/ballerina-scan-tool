package org.wso2.ballerina;

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

// Custom rules extension point for bal scan tool
public abstract class ExternalRules {
    public JsonArray externalIssues = new JsonArray();

    // This method is used by the bal scan tool internally to retrieve the custom issues
    public JsonArray getExternalIssues() {
        return externalIssues;
    }

    /**
     * This method should be used by custom tool plugins to implement new rules
     * To create custom rules the {@link io.ballerina.compiler.syntax.tree.NodeVisitor} can be extended
     */
    public abstract void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel);
}
