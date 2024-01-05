package org.wso2.ballerina.plugin;

import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;

public class CustomBallerinaRulesCompilerPlugin extends CompilerPlugin {
    @Override
    public void init(CompilerPluginContext pluginContext) {
        // TODO:
        //  1. Extend the CustomScanner interface here
        //  2. Change the META-INF to suite this new implementation
        // TODO: For testing purposes, have a print method here and get it to be service loaded
        // Basically implement compiler plugin
        // our interface Scan Tool interface for compiler plugin
        // Lang repo might have URL CLass loader
        // Block by block comment the issues while attempting the mock testing of the compiler plugins and the tool

        // Compiler phase to run analyzer on
        pluginContext.addCodeAnalyzer(new CustomBallerinaRulesCodeAnalyzer());
    }
}
