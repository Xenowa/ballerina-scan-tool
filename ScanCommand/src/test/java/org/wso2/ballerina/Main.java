package org.wso2.ballerina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageName;
import io.ballerina.projects.PackageOrg;
import io.ballerina.projects.PackageVersion;
import io.ballerina.projects.Project;
import io.ballerina.projects.SemanticVersion;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.projects.environment.PackageResolver;
import io.ballerina.projects.environment.ResolutionOptions;
import io.ballerina.projects.environment.ResolutionRequest;
import io.ballerina.projects.environment.ResolutionResponse;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticProperty;
import io.ballerina.tools.diagnostics.DiagnosticPropertyKind;
import org.wso2.ballerina.internal.utilities.ScanTomlFile;
import org.wso2.ballerina.internal.utilities.ScanToolConstants;
import org.wso2.ballerina.internal.utilities.ScanUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static io.ballerina.projects.util.ProjectConstants.IMPORT_PREFIX;
import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.projects.util.ProjectConstants.REPOSITORIES_DIR;
import static io.ballerina.projects.util.ProjectConstants.REPO_BALA_DIR_NAME;

public class Main {
    public static void main(String[] args) {
        // To set the ballerina home
        System.setProperty("ballerina.home", "C:\\Program Files\\Ballerina\\distributions\\ballerina-2201.8.5");

        ScanTomlFile scanTomlFile = ScanUtils.retrieveScanTomlConfigurations("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester");

        ArrayList<Issue> issues = new ArrayList<>();

        // TODO: Code generate custom compiler plugins from the information available in the scan.toml file
        // TODO: 1. Check if the scan.toml given compiler plugins exists in central repo or local repo
        Map<String, ScanTomlFile.Plugin> compilerPluginImports = new HashMap<>();
        Pattern versionPattern = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");
        scanTomlFile.getPlugins().forEach(plugin -> {
            if (versionPattern.matcher(plugin.getVersion()).matches()) {
                SemanticVersion version = SemanticVersion.from(plugin.getVersion());
                compilerPluginImports.putAll(resolvePackage(plugin, version));
            }
        });

        // TODO: 2. Generate them as imports to the main.bal file
        Project project = ProjectLoader.loadProject(Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester"));
        project.currentPackage().moduleIds().forEach(moduleId -> {
            Module module = project.currentPackage().module(moduleId);

            if (module.isDefaultModule()) {
                StringBuilder newImports = new StringBuilder();
                StringBuilder tomlDependencies = new StringBuilder();
                compilerPluginImports.forEach((pluginImport, plugin) -> {
                    newImports.append(pluginImport).append("\n");
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

                Document mainBal = module.document(module.documentIds().iterator().next());
                String documentContent = mainBal.textDocument().toString();
                mainBal = mainBal.modify().withContent(newImports + documentContent).apply();
                BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
                if (ballerinaToml != null) {
                    documentContent = ballerinaToml.tomlDocument().textDocument().toString();
                    ballerinaToml = ballerinaToml.modify().withContent(documentContent + tomlDependencies).apply();
                }
            }
        });

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scanTomlFile, ScanTomlFile.class);
        System.out.println(jsonOutput);
    }

    private static Map<String, ScanTomlFile.Plugin> resolvePackage(ScanTomlFile.Plugin plugin, SemanticVersion version) {
        Map<String, ScanTomlFile.Plugin> importAndPlugin = new HashMap<>();
        Path localRepoPath = ProjectUtils.createAndGetHomeReposPath();
        Path centralCachePath = localRepoPath.resolve(REPOSITORIES_DIR)
                .resolve(ProjectConstants.CENTRAL_REPOSITORY_CACHE_NAME);

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
                        + ScanToolConstants.PATH_SEPARATOR
                        + plugin.getName()
                        + ScanToolConstants.USE_IMPORT_AS_SERVICE, plugin);
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
                    + ScanToolConstants.PATH_SEPARATOR
                    + plugin.getName()
                    + ScanToolConstants.USE_IMPORT_AS_SERVICE, plugin);
            return importAndPlugin;
        }

        return resolveBalaPath(plugin, version);
    }

    private static Map<String, ScanTomlFile.Plugin> resolveBalaPath(ScanTomlFile.Plugin plugin, SemanticVersion version) {
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
                        + ScanToolConstants.PATH_SEPARATOR
                        + plugin.getName()
                        + ScanToolConstants.USE_IMPORT_AS_SERVICE, plugin);
                return importAndPlugin;
            }
        }
        return importAndPlugin;
    }
}
