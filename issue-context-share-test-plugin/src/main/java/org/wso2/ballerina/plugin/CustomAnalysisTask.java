package org.wso2.ballerina.plugin;

import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import org.wso2.ballerina.ToolAndCompilerPluginConnector;

public class CustomAnalysisTask extends ToolAndCompilerPluginConnector implements AnalysisTask<CompilationAnalysisContext> {
    @Override
    public void perform(CompilationAnalysisContext context) {
        // Analysis task
        System.out.println("Issues context share test plugin engaged!");

        // Print the message retrieved from bal scan tool during compilation
        System.out.println(getMessageFromTool());
    }
}
