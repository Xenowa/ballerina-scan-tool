package io.ballerina.scan;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.internal.parser.tree.STToken;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionSignatureNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;

import java.util.List;

import static io.ballerina.scan.InbuiltRules.INBUILT_RULES;
import static io.ballerina.scan.utilities.ScanToolConstants.CHECK_VIOLATION;
import static io.ballerina.scan.utilities.ScanToolConstants.CODE_SMELL;

public class StaticCodeAnalyzer extends NodeVisitor {

    private final Project currentProject;
    private final Module currentModule;
    private final Document currentDocument;
    private final SyntaxTree syntaxTree;
    private final SemanticModel semanticModel;
    private final InternalScannerContext scannerContext;

    public StaticCodeAnalyzer(Project currentProject,
                              Module currentModule,
                              Document currentDocument,
                              SyntaxTree syntaxTree,
                              SemanticModel semanticModel,
                              InternalScannerContext scannerContext) {

        this.currentProject = currentProject;
        this.currentModule = currentModule;
        this.currentDocument = currentDocument;
        this.syntaxTree = syntaxTree;
        this.semanticModel = semanticModel;
        this.scannerContext = scannerContext;
    }

    public void initialize() {
        // Go with the following approach similar to CodeAnalyzer
        this.visit((ModulePartNode) syntaxTree.rootNode());

        // Other method to start the visit
        // mainNode.accept(this);
    }

    @Override
    public void visit(FunctionSignatureNode functionSignatureNode) {
        // Perform the analysis
        int parameterCount = functionSignatureNode.parameters().size();
        int allowedParametersLimit = 7;
        if (parameterCount > allowedParametersLimit) {
            // Report issue
            scannerContext.getReporter().reportIssue(
                    functionSignatureNode.lineRange().startLine().line(),
                    functionSignatureNode.lineRange().startLine().offset(),
                    functionSignatureNode.lineRange().endLine().line(),
                    functionSignatureNode.lineRange().endLine().offset(),
                    INBUILT_RULES.get("S107").getRuleID(),
                    INBUILT_RULES.get("S107").getRuleDescription(),
                    CHECK_VIOLATION,
                    CODE_SMELL,
                    currentDocument,
                    currentModule,
                    currentProject);
        }

        // Continue visiting other nodes of the syntax tree
        this.visitSyntaxNode(functionSignatureNode);
    }

    @Override
    public void visit(FunctionBodyBlockNode functionBodyBlockNode) {

        functionBodyBlockNode.statements().forEach(statementNode -> {
            statementNode.children().forEach(childPair -> {
                List<STToken> tokens = childPair.internalNode().tokens();
                tokens.forEach(token -> {
                    if (token.kind.equals(SyntaxKind.CHECKPANIC_KEYWORD)) {
                        // Report the issue
                        scannerContext.getReporter().reportIssue(
                                childPair.lineRange().startLine().line(),
                                childPair.lineRange().startLine().offset(),
                                childPair.lineRange().endLine().line(),
                                childPair.lineRange().endLine().offset(),
                                INBUILT_RULES.get("S108").getRuleID(),
                                INBUILT_RULES.get("S108").getRuleDescription(),
                                CHECK_VIOLATION,
                                CODE_SMELL,
                                currentDocument,
                                currentModule,
                                currentProject
                        );
                    }
                });
            });
        });

        this.visitSyntaxNode(functionBodyBlockNode);
    }
}
