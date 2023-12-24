package org.wso2.ballerina.plugin;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.internal.diagnostics.SyntaxDiagnostic;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentConfig;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.ballerinalang.diagramutil.SyntaxTreeDiagnosticsUtil;
import org.wso2.ballerina.ExternalRules;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.plugin.checks.CustomChecks;

import java.util.List;

public class CustomCompilerPluginChecks extends ExternalRules implements AnalysisTask<SyntaxNodeAnalysisContext> {
    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        System.out.println("Custom rules compiler plugin is in action =)");

        // Simulating a dummy issue reporting
        CustomChecks customChecks = new CustomChecks(context.syntaxTree());
        customChecks.initialize(context, externalIssues);
    }

    // For now we will forget this method (This plugin will use the compiler instead of bal scan tool for the trees)
    @Override
    public void initialize(SyntaxTree syntaxTree, SemanticModel semanticModel) {
    }
}
