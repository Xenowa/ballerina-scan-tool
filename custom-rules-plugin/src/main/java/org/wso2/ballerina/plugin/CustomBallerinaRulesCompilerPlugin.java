package org.wso2.ballerina.plugin;

import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;

public class CustomBallerinaRulesCompilerPlugin extends CompilerPlugin {
    @Override
    public void init(CompilerPluginContext pluginContext) {
        // Compiler phase to run analyzer on
        pluginContext.addCodeAnalyzer(new CustomBallerinaRulesCodeAnalyzer());
    }
}
