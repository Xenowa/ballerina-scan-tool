package org.wso2.ballerina;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import java.util.ArrayList;

// Extension point for bal scan tool
public interface ExternalRules {
    /**
     * This method should be used to introduce custom ruleID's for the rules
     */
    ArrayList<String> setRules();

    /**
     * This method should be used by custom tool plugins to implement new rules
     * To create custom rules the {@link io.ballerina.compiler.syntax.tree.NodeVisitor} can be extended
     */
    void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel);

    // TODO:
    //  There should be an interface to provide the rule ID's
    //  There should be an interface to implement the actual rules using visitors
    //  There should be an interface to report all rule violations ✔✔✔
}
