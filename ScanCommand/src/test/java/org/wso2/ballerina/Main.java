package org.wso2.ballerina;

import io.ballerina.projects.JBallerinaBackend;
import io.ballerina.projects.JvmTarget;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;


import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        ScanTomlPropertiesManifest scanTomlPropertiesManifest = new ScanTomlPropertiesManifest();
        scanTomlPropertiesManifest.execute();
    }

    public static void executeCompilerPluginsThroughClassLoaders() {
        // Retrieving values from Ballerina.toml
        Path ballerinaProjectPath = Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester");
        Project project = ProjectLoader.loadProject(ballerinaProjectPath);

        // Testing the JBallerina backend (For accessing the class methods)
        PackageCompilation compilation = project.currentPackage().getCompilation();
        JBallerinaBackend jBallerinaBackend = JBallerinaBackend.from(compilation, JvmTarget.JAVA_17);
        Class<CustomScanner> customScanner;
        try {
            customScanner = (Class<CustomScanner>) jBallerinaBackend.jarResolver()
                    .getClassLoaderWithRequiredJarFilesForExecution()
                    .loadClass("CustomScanner");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
