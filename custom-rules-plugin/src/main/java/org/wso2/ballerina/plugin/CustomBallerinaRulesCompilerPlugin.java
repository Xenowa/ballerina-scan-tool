package org.wso2.ballerina.plugin;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;

import java.util.List;

public class CustomBallerinaRulesCompilerPlugin extends CodeAnalyzer {
    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        // For rules to check function body (This task will only run for function bodies)
        codeAnalysisContext.addSyntaxNodeAnalysisTask(new CustomCompilerPluginChecks(), SyntaxKind.FUNCTION_BODY_BLOCK);
    }
}
