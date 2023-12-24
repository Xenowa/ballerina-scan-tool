package org.wso2.ballerina.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.wso2.ballerina.ExternalRules;
import org.wso2.ballerina.plugin.checks.CustomChecks;

public class CustomCompilerPluginChecks extends ExternalRules implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        SyntaxTree syntaxTree = context.syntaxTree();

        // Perform custom rule analysis
        CustomChecks customChecks = new CustomChecks(syntaxTree);
        customChecks.initialize(externalIssues);
    }


    // For now we will forget this method (This plugin will use the compiler instead of bal scan tool for the trees)
    @Override
    public void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel) {

    }
}
