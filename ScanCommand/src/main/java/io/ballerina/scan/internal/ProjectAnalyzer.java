/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.CompilerPluginCache;
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
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.Severity;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextRange;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.MAIN_BAL;
import static io.ballerina.scan.internal.ScanToolConstants.PATH_SEPARATOR;
import static io.ballerina.scan.internal.ScanToolConstants.RULES_FILE;
import static io.ballerina.scan.internal.ScanToolConstants.USE_IMPORT_AS_SERVICE;

public class ProjectAnalyzer {

    private final ScanTomlFile scanTomlFile;

    ProjectAnalyzer(ScanTomlFile scanTomlFile) {
        this.scanTomlFile = scanTomlFile;
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
                                    } catch (MalformedURLException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                });
                                URLClassLoader ucl = new URLClassLoader(jarUrls.toArray(new URL[0]),
                                        this.getClass().getClassLoader());

                                // Obtain the rules from the resource file
                                InputStream resourceAsStream = ucl.getResourceAsStream(RULES_FILE);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                                        StandardCharsets.UTF_8));

                                // Parse the rules
                                StringBuilder stringBuilder = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    stringBuilder.append(line);
                                }
                                reader.close();
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                JsonArray ruleArray = gson.fromJson(stringBuilder.toString(), JsonArray.class);

                                // Generate in memory rules
                                ruleArray.forEach(rule -> {
                                    JsonObject ruleObject = rule.getAsJsonObject();
                                    int numericId = ruleObject.get("id").getAsInt();
                                    Severity severity = switch (ruleObject.get("severity").getAsString()) {
                                        case "BUG" -> Severity.BUG;
                                        case "VULNERABILITY" -> Severity.VULNERABILITY;
                                        case "CODE_SMELL" -> Severity.CODE_SMELL;
                                        default -> null;
                                    };
                                    String description = ruleObject.get("description").getAsString();

                                    // Create in memory rule objects
                                    if (severity != null) {
                                        Rule inMemoryRule = RuleFactory.createRule(numericId, description, severity,
                                                org.value(), name.value());
                                        externalRules.add(inMemoryRule);
                                    }
                                });
                            } catch (SecurityException | IllegalArgumentException | IOException ex) {
                                throw new RuntimeException(ex);
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
        InternalScannerContext internalScannerContext = new InternalScannerContext(allIssues,
                InbuiltRules.INBUILT_RULES);

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

        // Generating dependencies
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            documentContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(documentContent + tomlDependencies).apply();
        }

        // Passing scanner contexts to compiler plugins
        List<ScannerContext> scannerContextList = new ArrayList<>();
        PackageResolution packageResolution = project.currentPackage().getResolution();

        // Get the dependencies generated in the main.bal file
        ResolvedPackageDependency rootPkgNode = new ResolvedPackageDependency(project.currentPackage(),
                PackageDependencyScope.DEFAULT);

        List<Package> directDependencies = packageResolution.dependencyGraph()
                .getDirectDependencies(rootPkgNode)
                .stream()
                .map(ResolvedPackageDependency::packageInstance)
                .toList();

        for (Package pkgDependency : directDependencies) {
            PackageManifest pkgManifest = pkgDependency.manifest();

            PackageOrg org = pkgManifest.org();
            PackageName name = pkgManifest.name();
            String reportedSource = org + PATH_SEPARATOR + name;

            if (analyzerDescriptors.contains(reportedSource)) {
                // Get the URL of the imported compiler plugin
                List<String> jarPaths = new ArrayList<>();

                pkgManifest.compilerPluginDescriptor().ifPresent(pluginDesc -> {
                    // There is only 1 pluginDesc per compiler plugin
                    pluginDesc.dependencies().forEach(dependency -> {
                        jarPaths.add(dependency.getPath());
                    });

                    // Get fully qualified class name of the class implementing the compiler plugin
                    String fqn = pluginDesc.plugin().getClassName();

                    List<Rule> externalRules = new ArrayList<>();

                    // Create a URLClassLoader
                    List<URL> jarUrls = new ArrayList<>();
                    jarPaths.forEach(jarPath -> {
                        try {
                            URL jarUrl = Path.of(jarPath).toUri().toURL();
                            jarUrls.add(jarUrl);
                        } catch (MalformedURLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                    URLClassLoader ucl = new URLClassLoader(jarUrls.toArray(new URL[0]),
                            this.getClass().getClassLoader());

                    // Obtain the rules from the resource file
                    InputStream resourceAsStream = ucl.getResourceAsStream(RULES_FILE);

                    // Parse the rules
                    StringBuilder stringBuilder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                            StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    JsonArray ruleArray = gson.fromJson(stringBuilder.toString(), JsonArray.class);

                    // Generate in memory rules
                    ruleArray.forEach(rule -> {
                        JsonObject ruleObject = rule.getAsJsonObject();
                        int numericId = ruleObject.get("id").getAsInt();
                        Severity severity = switch (ruleObject.get("severity").getAsString()) {
                            case "BUG" -> Severity.BUG;
                            case "VULNERABILITY" -> Severity.VULNERABILITY;
                            case "CODE_SMELL" -> Severity.CODE_SMELL;
                            default -> null;
                        };
                        String description = ruleObject.get("description").getAsString();

                        // Create in memory rule objects
                        if (severity != null) {
                            Rule inMemoryRule = RuleFactory.createRule(numericId, description, severity,
                                    org.value(), name.value());
                            externalRules.add(inMemoryRule);
                        }
                    });

                    // Create and add scanner context to static analysis compiler plugins
                    ScannerContext context = new ScannerContextIml(externalRules);
                    scannerContextList.add(context);

                    Map<String, Object> pluginProperties = new HashMap<>();
                    pluginProperties.put("ScannerContext", context);

                    project.projectEnvironmentContext()
                            .getService(CompilerPluginCache.class)
                            .putData(fqn, pluginProperties);
                });
            }
        }

        // Engage custom compiler plugins through package compilation
        project.currentPackage().getCompilation();

        // Filter main bal file issues and remove imported compiler plugin lines from issues
        List<Issue> externalIssues = new ArrayList<>();
        scannerContextList.forEach(scannerContext -> {
            ReporterIml reporter = (ReporterIml) scannerContext.getReporter();
            externalIssues.addAll(reporter.getIssues());
        });

        List<Issue> modifiedExternalIssues = new ArrayList<>();
        externalIssues.forEach(issue -> {
            // Cast the external issue to its implementation to retrieve additional getters
            IssueIml externalIssueIml = (IssueIml) issue;
            if (externalIssueIml.fileName().equals(project.currentPackage()
                    .packageName()
                    + PATH_SEPARATOR
                    + MAIN_BAL)) {
                // Modify the issue
                LineRange lineRange = issue.location().lineRange();
                TextRange textRange = issue.location().textRange();
                BLangDiagnosticLocation modifiedLocation = new BLangDiagnosticLocation(lineRange.fileName(),
                        lineRange.startLine().line() - importCounter.get(),
                        lineRange.endLine().line() - importCounter.get(),
                        lineRange.startLine().offset(),
                        lineRange.endLine().offset(),
                        textRange.startOffset(),
                        textRange.length());
                IssueIml modifiedExternalIssue = new IssueIml(modifiedLocation, externalIssueIml.rule(),
                        externalIssueIml.source(), externalIssueIml.fileName(), externalIssueIml.filePath());

                modifiedExternalIssues.add(modifiedExternalIssue);
            } else {
                modifiedExternalIssues.add(issue);
            }
        });

        return modifiedExternalIssues;
    }
}
