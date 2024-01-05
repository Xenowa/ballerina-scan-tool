package org.wso2.ballerina.plugin;

import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CompilationAnalysisContext;
import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;
import org.wso2.ballerina.ToolAndCompilerPluginConnector;
import org.wso2.ballerina.internal.ReportLocalIssue;

public class CustomCompilerPlugin extends CompilerPlugin implements ToolAndCompilerPluginConnector {
    String messageFromTool = null;

    // Should be initialized before compiling
    @Override
    public void sendMessageFromTool(String messageFromTool) {
        this.messageFromTool = messageFromTool;
    }

    // Runs during compilation
    @Override
    public void init(CompilerPluginContext pluginContext) {
        pluginContext.addCodeAnalyzer(new CodeAnalyzer() {
            @Override
            public void init(CodeAnalysisContext codeAnalysisContext) {
                codeAnalysisContext.addCompilationAnalysisTask(new AnalysisTask<CompilationAnalysisContext>() {
                    @Override
                    public void perform(CompilationAnalysisContext compilationAnalysisContext) {
                        System.out.println("Issues context share test plugin engaged!");
                        System.out.println(messageFromTool);
                    }
                });
            }
        });
    }
}
