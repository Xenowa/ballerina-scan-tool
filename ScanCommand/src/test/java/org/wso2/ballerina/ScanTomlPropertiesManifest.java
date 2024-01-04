package org.wso2.ballerina;

import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.ast.TomlValueNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ScanTomlPropertiesManifest {
    public void execute() {
        Map<String, Map<String, Object>> scanToolConfigs = retrieveScanToolConfigs();
        Set<String> platformNames = scanToolConfigs != null ? scanToolConfigs.keySet() : null;
        if (platformNames != null) {
            platformNames.forEach(platformName -> {
                scanToolConfigs.get(platformName).forEach((propertyName, propertyValue) -> {
                    System.out.println(propertyName);
                    System.out.println(propertyValue);
                    System.out.println();
                });
            });
        }
    }

    private Map<String, Map<String, Object>> retrieveScanToolConfigs() {
        Path ballerinaProjectPath = Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester");
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

                    // Retrieve the platform table contents
                    Toml sonarqubeTable = scanTomlDocumentContent.getTable("sonarqube").orElse(null);
                    Toml semgrepTable = scanTomlDocumentContent.getTable("semgrep").orElse(null);
                    Toml codeqlTable = scanTomlDocumentContent.getTable("codeql").orElse(null);
                    Map<String, Map<String, Object>> platformProperties = new HashMap<>();


                    // Populate the platform properties if exists
                    if (sonarqubeTable != null) {
                        platformProperties.put("sonarqube", sonarqubeTable.toMap());
                    }
                    if (semgrepTable != null) {
                        platformProperties.put("semgrep", semgrepTable.toMap());
                    }
                    if (codeqlTable != null) {
                        platformProperties.put("codeql", codeqlTable.toMap());
                    }

                    // Return the populated map
                    return platformProperties;
                }
                return null;
            }
            return null;
        }
        return null;
    }
}

