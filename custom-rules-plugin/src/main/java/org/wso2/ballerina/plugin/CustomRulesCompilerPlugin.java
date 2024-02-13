package org.wso2.ballerina.plugin;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CompilerPluginContext;
import org.wso2.ballerina.internal.ScannerCompilerPlugin;
import org.wso2.ballerina.internal.ScannerContext;

import static org.wso2.ballerina.internal.utilities.ScanToolConstants.CODE_SMELL;

public class CustomRulesCompilerPlugin extends ScannerCompilerPlugin {
    @Override
    public void init(CompilerPluginContext compilerPluginContext) {
        compilerPluginContext.addCodeAnalyzer(new CodeAnalyzer() {
            @Override
            public void init(CodeAnalysisContext codeAnalysisContext) {
                codeAnalysisContext.addSyntaxNodeAnalysisTask(context -> {
                    // Get access to the current module
                    Module module = context.currentPackage().module(context.moduleId());
                    // Get access to the current document
                    Document document = module.document(context.documentId());
                    // Get access to the current project
                    Project project = module.project();

                    // Get the function body block node
                    FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) context.node();

                    // Perform the check on the body block node
                    // CUSTOM RULE: if function body is empty then report issue
                    if (functionBodyBlockNode.statements().isEmpty()) {
                        // Report issue through scanner context reporter
                        ScannerContext scannerContext = getScannerContext(compilerPluginContext);
                        scannerContext.getReporter().reportExternalIssue(
                                functionBodyBlockNode.lineRange().startLine().line(),
                                functionBodyBlockNode.lineRange().startLine().offset(),
                                functionBodyBlockNode.lineRange().endLine().line(),
                                functionBodyBlockNode.lineRange().endLine().offset(),
                                "Add a nested comment explaining why" +
                                        " this function is empty or complete the implementation.",
                                CODE_SMELL,
                                document,
                                module,
                                project
                        );
                    }

                    // Save the issues to file
                    saveExternalIssues(compilerPluginContext);
                }, SyntaxKind.FUNCTION_BODY_BLOCK);
            }
        });
    }
}