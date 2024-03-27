/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.ModuleCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.tools.text.LineRange;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.utilities.ScanToolConstants.MAIN_BAL;
import static io.ballerina.scan.utilities.ScanToolConstants.PATH_SEPARATOR;
import static io.ballerina.scan.utilities.ScanToolConstants.USE_IMPORT_AS_SERVICE;

public class ProjectAnalyzer {

    private final ScanTomlFile scanTomlFile;
    private final PrintStream outputStream;

    ProjectAnalyzer(ScanTomlFile scanTomlFile, PrintStream outputStream) {
        this.scanTomlFile = scanTomlFile;
        this.outputStream = outputStream;
    }

    public List<Issue> analyzeProject(Project project) {
        // Issues store
        List<Issue> allIssues = new ArrayList<>();
        InternalScannerContext internalScannerContext = new InternalScannerContext(allIssues);

        if (project.kind().equals(ProjectKind.SINGLE_FILE_PROJECT)) {
            Module tempModule = project.currentPackage().getDefaultModule();
            tempModule.documentIds().forEach(documentId -> {
                analyzeDocument(project, tempModule, documentId, internalScannerContext);
            });
        } else {
            // Iterate through each module of the project
            project.currentPackage().moduleIds().forEach(moduleId -> {
                // Get access to the project modules
                Module module = project.currentPackage().module(moduleId);

                // Iterate through each ballerina test file in a ballerina project and perform static analysis
                module.testDocumentIds().forEach(testDocumentID -> {
                    analyzeDocument(project, module, testDocumentID, internalScannerContext);
                });

                // Iterate through each document of the Main module/project + submodules
                module.documentIds().forEach(documentId -> {
                    analyzeDocument(project, module, documentId, internalScannerContext);
                });
            });
        }

        // return the detected issues
        return allIssues;
    }

    public void analyzeDocument(Project currentProject,
                                Module currentModule,
                                DocumentId documentId,
                                InternalScannerContext internalScannerContext) {
        // Retrieve each document from the module
        Document currentDocument = currentModule.document(documentId);

        // Retrieve syntax tree of each document
        SyntaxTree syntaxTree = currentDocument.syntaxTree();

        // Get semantic model from module compilation
        ModuleCompilation compilation = currentModule.getCompilation();
        SemanticModel semanticModel = compilation.getSemanticModel();

        // Perform core scans
        runInternalScans(currentDocument,
                syntaxTree,
                semanticModel,
                internalScannerContext);

        // Perform external scans
        runCustomScans(currentDocument, internalScannerContext);
    }

    public void runInternalScans(Document currentDocument,
                                 SyntaxTree syntaxTree,
                                 SemanticModel semanticModel,
                                 InternalScannerContext scannerContext) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(currentDocument,
                syntaxTree,
                semanticModel,
                scannerContext);

        analyzer.initialize();
    }

    public void runCustomScans(Document currentDocument, InternalScannerContext internalScannerContext) {
        // Run custom scans once
        if (currentDocument.module().isDefaultModule() && currentDocument.name().equals(MAIN_BAL)) {
            // Get the analyzer plugins as imports & generate them as toml dependencies if version is provided
            StringBuilder newImports = new StringBuilder();
            StringBuilder tomlDependencies = new StringBuilder();
            AtomicInteger importCounter = new AtomicInteger(0);

            scanTomlFile.getAnalyzers().forEach(analyzer -> {
                // Generate analyzer as import
                String analyzerImport = IMPORT_PREFIX + analyzer.getOrg() + PATH_SEPARATOR + analyzer.getName()
                        + USE_IMPORT_AS_SERVICE;
                newImports.append(analyzerImport).append("\n");

                // Generate toml dependencies if version provided
                if (analyzer.getVersion() != null) {
                    tomlDependencies.append("\n");
                    tomlDependencies.append("[[dependency]]" + "\n");
                    tomlDependencies.append("org='" + analyzer.getOrg() + "'\n");
                    tomlDependencies.append("name='" + analyzer.getName() + "'\n");
                    tomlDependencies.append("version='" + analyzer.getVersion() + "'\n");

                    if (analyzer.getRepository() != null) {
                        tomlDependencies.append("repository='" + analyzer.getRepository() + "'\n");
                    }

                    tomlDependencies.append("\n");
                }

                // Increment the imports counter
                importCounter.getAndIncrement();
            });

            // Generating imports
            String documentContent = currentDocument.textDocument().toString();
            currentDocument.modify().withContent(newImports + documentContent).apply();

            // Generating dependencies
            Project currentProject = currentDocument.module().project();
            BallerinaToml ballerinaToml = currentProject.currentPackage().ballerinaToml().orElse(null);
            if (ballerinaToml != null) {
                documentContent = ballerinaToml.tomlDocument().textDocument().toString();
                ballerinaToml.modify().withContent(documentContent + tomlDependencies).apply();
            }

            // TODO: External Scanner context will be used after property bag feature is introduced by project API
            //  External issues store
            //  List<Issue> externalIssues = new ArrayList<>();
            //  ScannerContext scannerContext = new ScannerContext(externalIssues);

            // Engage custom compiler plugins through module compilation
            currentProject.currentPackage().getCompilation();

            // Retrieve External issues
            List<Issue> externalIssues = StaticCodeAnalyzerPlugin.getIssues();

            if (externalIssues != null) {
                // Filter main bal file which compiler plugin imports were generated and remove imported lines from
                // reported issues and create a modified external issues array
                List<Issue> modifiedExternalIssues = new ArrayList<>();
                externalIssues.forEach(externalIssue -> {
                    // Cast the external issue to its implementation to retrieve additional getters
                    IssueIml externalIssueIml = (IssueIml) externalIssue;
                    if (externalIssueIml.getFileName().equals(currentProject.currentPackage()
                            .packageName()
                            + PATH_SEPARATOR
                            + MAIN_BAL)) {
                        // Modify the issue
                        LineRange lineRange = externalIssueIml.getLocation().lineRange();
                        IssueIml modifiedExternalIssue = new IssueIml(
                                lineRange.startLine().line() - importCounter.get(),
                                lineRange.startLine().offset(),
                                lineRange.endLine().line() - importCounter.get(),
                                lineRange.endLine().offset(),
                                externalIssueIml.getRuleID(),
                                externalIssueIml.getMessage(),
                                externalIssueIml.getIssueSeverity(),
                                externalIssueIml.getIssueType(),
                                externalIssueIml.getFileName(),
                                externalIssueIml.getReportedFilePath(),
                                externalIssueIml.getReportedSource());

                        modifiedExternalIssues.add(modifiedExternalIssue);
                    } else {
                        modifiedExternalIssues.add(externalIssue);
                    }
                });

                internalScannerContext.getReporter().addExternalIssues(modifiedExternalIssues);
                outputStream.println("Running custom scanner plugins...");
            }
        }
    }
}
