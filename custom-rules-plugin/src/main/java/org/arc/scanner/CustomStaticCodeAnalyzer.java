package org.arc.scanner;

import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.projects.plugins.CompilerPluginContext;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.StaticCodeAnalyzerPlugin;

import static io.ballerina.scan.utilities.ScanToolConstants.CODE_SMELL;

public class CustomStaticCodeAnalyzer extends StaticCodeAnalyzerPlugin {

    @Override
    public void init(CompilerPluginContext compilerPluginContext) {

        compilerPluginContext.addCodeAnalyzer(new CodeAnalyzer() {
            @Override
            public void init(CodeAnalysisContext codeAnalysisContext) {

                codeAnalysisContext.addSyntaxNodeAnalysisTask(context -> {
                    Module module = context.currentPackage().module(context.moduleId());
                    Document document = module.document(context.documentId());
                    Project project = module.project();

                    FunctionBodyBlockNode functionBodyBlockNode = (FunctionBodyBlockNode) context.node();

                    // CUSTOM RULE: if function body is empty then report issue
                    if (functionBodyBlockNode.statements().isEmpty()) {
                        ScannerContext scannerContext = getScannerContext(compilerPluginContext);
                        scannerContext.getReporter().reportIssue(
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

                        complete();
                    }
                }, SyntaxKind.FUNCTION_BODY_BLOCK);
            }
        });
    }
}
