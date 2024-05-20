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
import io.ballerina.cli.launcher.CustomToolClassLoader;
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
import io.ballerina.scan.Rule;
import io.ballerina.scan.Severity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final PrintStream outputStream = System.out;

    public static void main(String[] args) {
        try {
            readExternalRules();
        } catch (Exception e) {
            outputStream.println(e.getMessage());
        }
        resolveProjectDependenciesAfterModification();
    }

    private static void resolveProjectDependenciesAfterModification() {
        // 1. Load the Ballerina project
        Project project = BuildProject.load(Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\sample_project"));

        // 2. define the local module to be used for the project
        StringBuilder tomlDependencies = new StringBuilder();
        tomlDependencies.append("[[dependency]]" + "\n");
        tomlDependencies.append("org='" + "ballerina" + "'\n");
        tomlDependencies.append("name='" + "io" + "'\n");
        tomlDependencies.append("version='" + "1.5.0" + "'\n");

        // 3. Retrieve Ballerina.toml file and generate dependency
        String tomlDocumentContent = "";
        BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().orElse(null);
        if (ballerinaToml != null) {
            tomlDocumentContent = ballerinaToml.tomlDocument().textDocument().toString();
            ballerinaToml.modify().withContent(tomlDocumentContent + tomlDependencies).apply();
        }

        // 4. Retrieve the deault module and generate the dependency as an import
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

    private static void readExternalRules() throws IOException {
        // Get the rules
        List<URL> urls = new ArrayList<>();
        urls.add(
                Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\ballerina-scan-tool" +
                                "\\sample-custom-rules-analyzer\\build\\libs\\sample-custom-rules-analyzer-0.1.0.jar")
                        .toUri()
                        .toURL());
        CustomToolClassLoader cl = new CustomToolClassLoader(urls.toArray(new URL[0]), Main.class.getClassLoader());
        InputStream resourceAsStream = cl.getResourceAsStream("rules.json");

        // Read the rules
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        // Convert string to JSON Array
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray ruleArray = gson.fromJson(stringBuilder.toString(), JsonArray.class);

        // Generate rules from the array
        String org = "tharanawanigaratne";
        String name = "custom_rules_analyzer";
        List<Rule> rules = new ArrayList<>();

        ruleArray.forEach(rule -> {
            // Parse the rule objects
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
                Rule inMemoryRule = RuleFactory.createRule(numericId, description, severity, org, name);
                rules.add(inMemoryRule);
            }
        });

        rules.forEach(rule -> {
            outputStream.println(gson.toJson(rule));
        });
    }
}
