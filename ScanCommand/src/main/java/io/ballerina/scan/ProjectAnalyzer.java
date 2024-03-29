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
import io.ballerina.projects.PackageDependencyScope;
import io.ballerina.projects.PackageManifest;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageResolution;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.tools.text.LineRange;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
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

    public List<Rule> getExternalAnalyzerRules(Project project) {
        List<Rule> externalRules = new ArrayList<>();

        Module defaultModule = project.currentPackage().getDefaultModule();
        Document mainBAL = defaultModule.document(defaultModule.documentIds().iterator().next());

        // Get the analyzer plugins as imports & generate them as toml dependencies if version is provided
        StringBuilder newImports = new StringBuilder();
        StringBuilder tomlDependencies = new StringBuilder();
        AtomicInteger importCounter = new AtomicInteger(0);

        List<String> analyzerDescriptors = new ArrayList<>();
        scanTomlFile.getAnalyzers().forEach(analyzer -> {
            // Generate analyzer as import
            String analyzerImport = IMPORT_PREFIX + analyzer.getOrg() + PATH_SEPARATOR + analyzer.getName()
                    + USE_IMPORT_AS_SERVICE;
            newImports.append(analyzerImport).append("\n");

            analyzerDescriptors.add(analyzer.getOrg() + PATH_SEPARATOR + analyzer.getName());

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
        String documentContent = mainBAL.textDocument().toString();
        mainBAL.modify().withContent(newImports + documentContent).apply();

        // Get direct dependencies of in memory BAL file through project API
        PackageResolution packageResolution = project.currentPackage().getResolution();
        ResolvedPackageDependency rootPkgNode = new ResolvedPackageDependency(project.currentPackage(),
                PackageDependencyScope.DEFAULT);

        List<Package> directDependencies = packageResolution.dependencyGraph()
                .getDirectDependencies(rootPkgNode)
                .stream()
                .map(ResolvedPackageDependency::packageInstance)
                .toList();

        // Load rules from compiler plugins found in imports of main BAL file
        for (Package pkgDependency : directDependencies) {
            PackageManifest pkgManifest = pkgDependency.manifest();

            // Retrieve org and name of each import
            PackageOrg org = pkgManifest.org();
            PackageName name = pkgManifest.name();

            // Check if import is a compiler plugin
            pkgManifest.compilerPluginDescriptor()
                    .ifPresent(pluginDesc -> {

                        // Check if compiler plugin is one defined in Scan.toml file by comparing org and name
                        String fqn = pluginDesc.plugin().getClassName();
                        String reportedSource = org + PATH_SEPARATOR + name;
                        if (analyzerDescriptors.contains(reportedSource)) {
                            try {
                                // Get the URL of the imported compiler plugin
                                List<String> jarPaths = new ArrayList<>();

                                // There is only 1 pluginDesc per compiler plugin
                                pluginDesc.dependencies().forEach(dependency -> {
                                    jarPaths.add(dependency.getPath());
                                });

                                // Create a URLClassLoader
                                List<URL> jarUrls = new ArrayList<>();
                                jarPaths.forEach(jarPath -> {
                                    try {
                                        URL jarUrl = Path.of(jarPath).toUri().toURL();
                                        jarUrls.add(jarUrl);
                                    } catch (MalformedURLException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
                                URLClassLoader ucl = new URLClassLoader(jarUrls.toArray(new URL[0]),
                                        this.getClass().getClassLoader());

                                // Load the class dynamically using the UCL
                                Class<?> pluginClass = ucl.loadClass(fqn);
                                StaticCodeAnalyzerPlugin plugin = (StaticCodeAnalyzerPlugin) pluginClass
                                        .getConstructor()
                                        .newInstance();

                                // Collect all rules from the compiler plugin
                                externalRules.addAll(plugin.rules());
                            } catch (ClassNotFoundException |
                                     NoSuchMethodException |
                                     SecurityException |
                                     InstantiationException |
                                     IllegalAccessException |
                                     IllegalArgumentException |
                                     InvocationTargetException e) {
                                // Handle any exceptions that might occur during class loading or method invocation
                                outputStream.println("Error loading or calling rules() method from compiler plugin: " +
                                        fqn);
                                outputStream.println(e.getMessage());
                            }
                        }
                    });
        }

        // Replace mainBAL file with its original content once to preserve initial line numbers during core scans
        mainBAL.modify().withContent(documentContent).apply();

        return externalRules;
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
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(currentDocument,
                syntaxTree,
                semanticModel,
                internalScannerContext);

        analyzer.initialize();
    }

    public List<Issue> runExternalAnalyzers(Project project) {
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document mainBAL = defaultModule.document(defaultModule.documentIds().iterator().next());

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
        String documentContent = mainBAL.textDocument().toString();
        mainBAL.modify().withContent(newImports + documentContent).apply();

        // Generating dependencies
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            documentContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(documentContent + tomlDependencies).apply();
        }

        // Engage custom compiler plugins through module compilation
        project.currentPackage().getCompilation();

        // Retrieve External issues
        List<Issue> externalIssues = StaticCodeAnalyzerPlugin.getIssues();

        if (externalIssues != null) {
            // Filter main bal file which compiler plugin imports were generated and remove imported lines from
            // reported issues and create a modified external issues array
            List<Issue> modifiedExternalIssues = new ArrayList<>();
            externalIssues.forEach(externalIssue -> {
                // Cast the external issue to its implementation to retrieve additional getters
                IssueIml externalIssueIml = (IssueIml) externalIssue;
                if (externalIssueIml.getFileName().equals(project.currentPackage()
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

            outputStream.println("Running custom scanner plugins...");
            return modifiedExternalIssues;
        }
        return new ArrayList<>();
    }
}
