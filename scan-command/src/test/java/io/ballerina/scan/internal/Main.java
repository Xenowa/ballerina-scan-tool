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

import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageDependencyScope;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.PackageManifest;
import io.ballerina.projects.PackageResolution;
import io.ballerina.projects.Project;
import io.ballerina.projects.ResolvedPackageDependency;
import io.ballerina.projects.directory.BuildProject;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public class Main {

    private static final PrintStream outputStream = System.out;

    public static void main(String[] args) {
        // 1. Load the Ballerina project
        Project project = BuildProject.load(Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\sample_project"));

        // 2. define the local module to be used for the project
        String moduleDependencyVersion = """
                [[dependency]]
                org="ballerina"
                name="io"
                version="1.5.0"
                """;

        // 3. Retrieve Ballerina.toml file and generate dependency
        String tomlDocumentContent = "";
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            tomlDocumentContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(tomlDocumentContent + moduleDependencyVersion).apply();
        }

        // 4. Retrieve the default module and generate the dependency as an import
        String dependencyAsImport = "import ballerina/io as _;\n";
        Module defaultModule = project.currentPackage().getDefaultModule();
        Document document = defaultModule.document(defaultModule.documentIds().iterator().next());
        String documentContent = document.textDocument().toString();
        document.modify().withContent(dependencyAsImport + documentContent).apply();

        // 5. Get direct dependencies of the default module file
        PackageResolution packageResolution = project.currentPackage().getResolution();
        ResolvedPackageDependency rootPkgNode = new ResolvedPackageDependency(project.currentPackage(),
                PackageDependencyScope.DEFAULT);

        List<Package> directDependencies = packageResolution.dependencyGraph()
                .getDirectDependencies(rootPkgNode)
                .stream()
                .map(ResolvedPackageDependency::packageInstance)
                .toList();

        // 6. Print the information of the dependencies in use
        directDependencies.forEach(dependency -> {
            PackageManifest manifest = dependency.manifest();
            PackageDescriptor descriptor = manifest.descriptor();
            outputStream.println("descriptor info: "
                    + descriptor.org() + "/"
                    + descriptor.name() + ":"
                    + descriptor.version());
        });
    }
}
