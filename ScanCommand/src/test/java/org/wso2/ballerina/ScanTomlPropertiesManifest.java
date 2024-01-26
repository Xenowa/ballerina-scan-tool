package org.wso2.ballerina;

import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.ast.TomlValueNode;
import org.wso2.ballerina.internal.utilities.ScanTomlFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ScanTomlPropertiesManifest {
    ScanTomlFile retrieveScanToolConfigs(String projectPath) {
        Path ballerinaProjectPath = Path.of(projectPath);
        Project project = ProjectLoader.loadProject(ballerinaProjectPath);

        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            // Retrieve the Ballerina.toml from the Ballerina project
            BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().get();

            // Retrieve it as a document
            TomlDocument ballerinaTomlDocument = ballerinaToml.tomlDocument();

            // Parse the toml document
            Toml ballerinaTomlDocumentContent = ballerinaTomlDocument.toml();

            // Retrieve only the [Scan] Table values
            Toml scanTable = ballerinaTomlDocumentContent.getTable("scan").orElse(null);

            if (scanTable != null) {
                // Retrieve the Scan.toml file path
                TomlValueNode tomlValue = scanTable.get("configPath").orElse(null);

                if (tomlValue != null) {
                    String scanTomlFilePath = (String) tomlValue.toNativeValue();

                    // Parse the toml document
                    Toml scanTomlDocumentContent;
                    try {
                        scanTomlDocumentContent = Toml.read(Path.of(scanTomlFilePath));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    // Start creating the Scan.toml object
                    ScanTomlFile scanTomlFile = new ScanTomlFile();

                    // Retrieve all platform tables
                    List<Toml> platformsTable = scanTomlDocumentContent.getTables("platform");
                    platformsTable.forEach(platformTable -> {
                        Map<String, Object> properties = platformTable.toMap();
                        String name = (String) properties.remove("name");
                        String path = (String) properties.remove("path");

                        if (name != null && Files.exists(Path.of(path))) {
                            ScanTomlFile.Platform platform = new ScanTomlFile.Platform(name, path, properties);
                            scanTomlFile.setPlatform(platform);
                        }
                    });

                    // Retrieve all custom rule compiler plugin tables
                    List<Toml> compilerPluginsTable = scanTomlDocumentContent.getTables("plugin");
                    compilerPluginsTable.forEach(compilerPluginTable -> {
                        Map<String, Object> properties = compilerPluginTable.toMap();
                        String org = (String) properties.get("org");
                        String name = (String) properties.get("name");
                        String version = (String) properties.get("version");

                        if (org != null && name != null && version != null) {
                            ScanTomlFile.Plugin plugin = new ScanTomlFile.Plugin(org, name, version);
                            scanTomlFile.setPlugin(plugin);
                        }
                    });

                    // Retrieve all filter rule tables
                    List<Toml> filterRulesTable = scanTomlDocumentContent.getTables("rule");
                    filterRulesTable.forEach(filterRuleTable -> {
                        Map<String, Object> properties = filterRuleTable.toMap();
                        String id = (String) properties.get("id");
                        if (id != null) {
                            ScanTomlFile.RuleToFilter ruleToFilter = new ScanTomlFile.RuleToFilter(id);
                            scanTomlFile.setRuleToFilter(ruleToFilter);
                        }
                    });

                    // Return the populated map
                    return scanTomlFile;
                }
                return null;
            }
            return null;
        }
        return null;
    }
}

