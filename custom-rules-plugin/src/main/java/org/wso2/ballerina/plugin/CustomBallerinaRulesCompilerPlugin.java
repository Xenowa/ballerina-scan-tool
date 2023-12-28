package org.wso2.ballerina.plugin;

import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;

public class CustomBallerinaRulesCompilerPlugin extends CodeAnalyzer {
    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        // Analyzer task to run during a single compilation
        codeAnalysisContext.addCompilationAnalysisTask(new CustomCompilerPlugin());
    }
}
