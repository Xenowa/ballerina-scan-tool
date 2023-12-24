package org.wso2.ballerina.plugin;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;

import java.util.List;

public class CustomBallerinaRulesCompilerPlugin extends CodeAnalyzer {
    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        // For rules to check function body (This task will only run when it detects module parts)
        codeAnalysisContext.addSyntaxNodeAnalysisTask(new CustomCompilerPluginChecks(), SyntaxKind.MODULE_PART);
    }
}
