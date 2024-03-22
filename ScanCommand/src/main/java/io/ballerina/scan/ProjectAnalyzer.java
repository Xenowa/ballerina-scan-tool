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
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.tools.text.LineRange;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static io.ballerina.projects.util.ProjectConstants.CENTRAL_REPOSITORY_CACHE_NAME;
import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.projects.util.ProjectConstants.REPOSITORIES_DIR;
import static io.ballerina.projects.util.ProjectConstants.REPO_BALA_DIR_NAME;
import static io.ballerina.scan.utilities.ScanToolConstants.CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN;
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

    public ArrayList<Issue> analyzeProject(Project project) {
        // Issues store
        ArrayList<Issue> allIssues = new ArrayList<>();
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
            Project currentProject = currentDocument.module().project();
            // TODO: External Scanner context will be used after property bag feature is introduced by project API
            //  External issues store
            //  ArrayList<Issue> externalIssues = new ArrayList<>();
            //  ScannerContext scannerContext = new ScannerContext(externalIssues);

            // Checking if compiler plugins provided in Scan.toml exists (This might not be mandatory)
            Map<String, ScanTomlFile.Plugin> compilerPluginImports = new HashMap<>();
            Pattern versionPattern = Pattern.compile(CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN);
            scanTomlFile.getPlugins().forEach(plugin -> {
                if (versionPattern.matcher(plugin.getVersion()).matches()) {
                    SemanticVersion version = SemanticVersion.from(plugin.getVersion());
                    compilerPluginImports.putAll(resolvePackage(plugin, version));
                }
            });

            // Converting compiler plugins as imports and dependencies
            StringBuilder newImports = new StringBuilder();
            StringBuilder tomlDependencies = new StringBuilder();
            AtomicInteger importCounter = new AtomicInteger(0);
            compilerPluginImports.forEach((pluginImport, plugin) -> {
                newImports.append(pluginImport).append("\n");
                importCounter.getAndIncrement();
                if (plugin.getRepository() != null && plugin.getRepository().equals(LOCAL_REPOSITORY_NAME)) {
                    tomlDependencies.append("\n");
                    tomlDependencies.append("[[dependency]]" + "\n");
                    tomlDependencies.append("version='" + plugin.getVersion() + "'\n");
                    tomlDependencies.append("org='" + plugin.getOrg() + "'\n");
                    tomlDependencies.append("name='" + plugin.getName() + "'\n");
                    tomlDependencies.append("repository='" + plugin.getRepository() + "'\n");
                    tomlDependencies.append("\n");
                }
            });

            // Generating imports

            // Since there can multiple files in default module
            String documentContent = currentDocument.textDocument().toString();
            currentDocument.modify().withContent(newImports + documentContent).apply();

            // Generating dependencies
            BallerinaToml ballerinaToml = currentProject.currentPackage().ballerinaToml().orElse(null);
            if (ballerinaToml != null) {
                documentContent = ballerinaToml.tomlDocument().textDocument().toString();
                ballerinaToml.modify().withContent(documentContent + tomlDependencies).apply();
            }

            // Engage custom compiler plugins through module compilation
            currentProject.currentPackage().getCompilation();

            // Retrieve External issues
            ArrayList<Issue> externalIssues = StaticCodeAnalyzerPlugin.getIssues();

            if (externalIssues != null) {
                // Filter main bal file which compiler plugin imports were generated and remove imported lines from
                // reported issues and create a modified external issues array
                ArrayList<Issue> modifiedExternalIssues = new ArrayList<>();
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

    private Map<String, ScanTomlFile.Plugin> resolvePackage(ScanTomlFile.Plugin plugin, SemanticVersion version) {

        Map<String, ScanTomlFile.Plugin> importAndPlugin = new HashMap<>();
        Path localRepoPath = ProjectUtils.createAndGetHomeReposPath();
        Path centralCachePath = localRepoPath.resolve(REPOSITORIES_DIR)
                .resolve(CENTRAL_REPOSITORY_CACHE_NAME);

        // Avoids pulling from central if user has provided the repository as local
        if (plugin.getRepository() != null && plugin.getRepository().equals(LOCAL_REPOSITORY_NAME)) {
            Path packagePathInLocalRepo = localRepoPath.resolve(REPOSITORIES_DIR)
                    .resolve(LOCAL_REPOSITORY_NAME)
                    .resolve(REPO_BALA_DIR_NAME)
                    .resolve(plugin.getOrg())
                    .resolve(plugin.getName())
                    .resolve(version.toString());
            if (Files.exists(packagePathInLocalRepo) && Files.isDirectory(packagePathInLocalRepo)) {
                importAndPlugin.put(IMPORT_PREFIX
                        + plugin.getOrg()
                        + PATH_SEPARATOR
                        + plugin.getName()
                        + USE_IMPORT_AS_SERVICE, plugin);
                return importAndPlugin;
            }
        }

        // Avoids pulling from central if the package version already available in the local central cache.
        Path packagePathInCentralCache = centralCachePath.resolve(plugin.getOrg())
                .resolve(plugin.getName())
                .resolve(version.toString());
        if (Files.exists(packagePathInCentralCache) && Files.isDirectory(packagePathInCentralCache)) {
            importAndPlugin.put(IMPORT_PREFIX
                    + plugin.getOrg()
                    + PATH_SEPARATOR
                    + plugin.getName()
                    + USE_IMPORT_AS_SERVICE, plugin);
            return importAndPlugin;
        }

        return resolveBalaPath(plugin, version);
    }

    private Map<String, ScanTomlFile.Plugin> resolveBalaPath(ScanTomlFile.Plugin plugin, SemanticVersion version) {

        Map<String, ScanTomlFile.Plugin> importAndPlugin = new HashMap<>();
        PackageDescriptor packageDescriptor = PackageDescriptor.from(PackageOrg.from(plugin.getOrg()),
                PackageName.from(plugin.getName()),
                PackageVersion.from(version), LOCAL_REPOSITORY_NAME);
        ResolutionRequest resolutionRequest = ResolutionRequest.from(packageDescriptor);

        PackageResolver packageResolver = EnvironmentBuilder.buildDefault().getService(PackageResolver.class);
        Collection<ResolutionResponse> resolutionResponses = packageResolver.resolvePackages(
                Collections.singletonList(resolutionRequest), ResolutionOptions.builder().setOffline(false).build());
        ResolutionResponse resolutionResponse = resolutionResponses.stream().findFirst().orElse(null);

        if (resolutionResponse != null &&
                resolutionResponse.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.RESOLVED)) {
            Package resolvedPackage = resolutionResponse.resolvedPackage();
            if (resolvedPackage != null) {
                importAndPlugin.put(IMPORT_PREFIX
                        + plugin.getOrg()
                        + PATH_SEPARATOR
                        + plugin.getName()
                        + USE_IMPORT_AS_SERVICE, plugin);
                return importAndPlugin;
            }
        }
        return importAndPlugin;
    }
}
