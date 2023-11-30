package org.wso2.ballerina.plugin;

import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.ExternalRules;
import org.wso2.ballerina.plugin.checks.CustomChecks;

import java.util.ArrayList;

public class CustomBallerinaPlugin extends ExternalRules {
    public void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel) {
        // Perform the check
        CustomChecks customChecks = new CustomChecks(syntaxTree);
        customChecks.initialize(externalIssues);
    }
}
