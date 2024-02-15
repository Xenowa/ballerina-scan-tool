package org.wso2.ballerina.internal;

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
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.util.ProjectUtils;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.internal.utilities.ScanTomlFile;

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
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.MAIN_BAL;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.PATH_SEPARATOR;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.USE_IMPORT_AS_SERVICE;

public class ProjectAnalyzer {
    private ScanTomlFile scanTomlFile = null;

    public ArrayList<Issue> analyzeProject(Path userPath, ScanTomlFile scanTomlFile) {
        // Set the scan toml file received
        this.scanTomlFile = scanTomlFile;

        // Array to hold all issues
        ArrayList<Issue> issues = new ArrayList<>();

        // Get access to the project API
        Project project = ProjectLoader.loadProject(userPath);

        // For single file inputs with bal scan
        if (!userPath.toFile().isDirectory()) {
            // if the user provided file path belongs to a build project stop the analysis
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                return null;
            }

            Module tempModule = project.currentPackage().getDefaultModule();
            DocumentId documentId = project.documentId(userPath);
            ArrayList<Issue> detectedIssues = analyzeDocument(project, tempModule, documentId);
            issues.addAll(detectedIssues);
        } else {
            // Iterate through each module of the project
            project.currentPackage().moduleIds().forEach(moduleId -> {
                // Get access to the project modules
                Module module = project.currentPackage().module(moduleId);

                // Iterate through each ballerina test file in a ballerina project and perform static analysis
                module.testDocumentIds().forEach(testDocumentID -> {
                    ArrayList<Issue> detectedIssues = analyzeDocument(project, module, testDocumentID);
                    issues.addAll(detectedIssues);
                });

                // Iterate through each document of the Main module/project + submodules
                module.documentIds().forEach(documentId -> {
                    ArrayList<Issue> detectedIssues = analyzeDocument(project, module, documentId);
                    issues.addAll(detectedIssues);
                });
            });
        }

        // return the detected issues
        return issues;
    }

    public ArrayList<Issue> analyzeDocument(Project currentProject, Module currentModule, DocumentId documentId) {
        // Retrieve each document from the module
        Document currentDocument = currentModule.document(documentId);

        // Map to store the parsed & Compiled outputs
        Map<String, Object> compiledOutputs = new HashMap<>();

        // Retrieve the syntax tree from the parsed ballerina document
        compiledOutputs.put("syntaxTree", currentDocument.syntaxTree());

        // Retrieve the compilation of the module
        ModuleCompilation compilation = currentModule.getCompilation();

        // Retrieve the semantic model from the ballerina document compilation
        compiledOutputs.put("semanticModel", compilation.getSemanticModel());

        // Perform static code analysis
        // Array to hold analysis issues for each document
        ArrayList<Issue> internalIssues = new ArrayList<>();

        // Set up a scanner context for each document being scanned for static code analysis
        ScannerContext scannerContext = new ScannerContext(internalIssues,
                currentDocument,
                currentModule,
                currentProject);

        // Perform internal static code analysis
        runInternalScans((SyntaxTree) compiledOutputs.get("syntaxTree"),
                (SemanticModel) compiledOutputs.get("semanticModel"),
                scannerContext);

        runCustomScans(currentDocument, currentProject, scannerContext);

        // Return the analyzed file results
        return internalIssues;
    }

    // For rules that can be implemented using the syntax tree model
    public void runInternalScans(SyntaxTree syntaxTree, SemanticModel semanticModel, ScannerContext scannerContext) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize(scannerContext);
    }

    public void runCustomScans(Document currentDocument, Project currentProject, ScannerContext scannerContext) {
        // Currently for each document in the default module it will run the custom scan which should be avoided
        if (currentDocument.module().isDefaultModule() && currentDocument.name().equals(MAIN_BAL)) {
            // Checking if compiler plugins provided in Scan.toml exists
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
            ArrayList<Issue> externalIssues = ScannerCompilerPlugin.getExternalIssues();

            if (externalIssues != null) {
                // Filter main bal file which compiler plugin imports were generated and remove imported lines from
                // reported issues and create a modified external issues array
                ArrayList<Issue> modifiedExternalIssues = new ArrayList<>();
                externalIssues.forEach(externalIssue -> {
                    if (externalIssue.getFileName().equals(currentProject.currentPackage()
                            .packageName()
                            + PATH_SEPARATOR
                            + MAIN_BAL)) {
                        // Modify the issue
                        Issue modifiedExternalIssue = new Issue(
                                externalIssue.getStartLine() - importCounter.get(),
                                externalIssue.getStartLineOffset(),
                                externalIssue.getEndLine() - importCounter.get(),
                                externalIssue.getEndLineOffset(),
                                externalIssue.getRuleID(),
                                externalIssue.getMessage(),
                                externalIssue.getIssueType(),
                                externalIssue.getType(),
                                externalIssue.getFileName(),
                                externalIssue.getReportedFilePath()
                        );
                        modifiedExternalIssues.add(modifiedExternalIssue);
                    } else {
                        modifiedExternalIssues.add(externalIssue);
                    }
                });

                scannerContext.getReporter().addExternalIssues(modifiedExternalIssues);
            } else {
                System.out.println("Unable to report external issues from: " +
                        currentProject.currentPackage().packageName());
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

        if (resolutionResponse != null && resolutionResponse.resolutionStatus().equals(ResolutionResponse.ResolutionStatus.RESOLVED)) {
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
