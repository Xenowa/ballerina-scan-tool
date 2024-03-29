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

package io.ballerina.scan.platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.scan.Issue;
import io.ballerina.scan.PlatformPluginContext;
import io.ballerina.scan.StaticCodeAnalysisPlatformPlugin;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SonarPlatformPlugin implements StaticCodeAnalysisPlatformPlugin {

    private PlatformPluginContext platformPluginContext;
    private final List<String> processBuilderArguments = new ArrayList<>();
    private final ProcessBuilder processBuilder = new ProcessBuilder();
    private final PrintStream outputStream = System.out;
    private static final String ISSUES_FILE_PATH = "ballerina-analysis-results.json";

    @Override
    public String platform() {
        return "sonarqube";
    }

    @Override
    public void init(PlatformPluginContext platformPluginContext) {
        this.platformPluginContext = platformPluginContext;

        // Initializing sonar-scanner cli
        if (SystemUtils.IS_OS_WINDOWS) {
            processBuilderArguments.add("cmd");
            processBuilderArguments.add("/c");
        } else {
            processBuilderArguments.add("sh");
            processBuilderArguments.add("-c");
        }
        processBuilderArguments.add("sonar-scanner");

        // Set the property to only scan ballerina files when the scan is triggered
        processBuilderArguments.add("-Dsonar.exclusions=" +
                "'" +
                "**/*.java," +
                "**/*.xml," +
                "**/*.yaml," +
                "**/*.go," +
                "**/*.kt," +
                "**/*.js," +
                "**/*.html," +
                "**/*.YAML" +
                ",**/*.rb," +
                "**/*.scala," +
                "**/*.py" +
                "'");
    }

    @Override
    public void onScan(List<Issue> issues) {
        saveIssues(ISSUES_FILE_PATH, issues);

        if (!platformPluginContext.initiatedByPlatform()) {
            processBuilderArguments.add("-DanalyzedResultsPath=" + Path.of(ISSUES_FILE_PATH).toAbsolutePath());

            String sonarProjectPropertiesPath = platformPluginContext.platformArgs()
                    .getOrDefault("sonarProjectPropertiesPath", null);
            if (sonarProjectPropertiesPath != null) {
                processBuilderArguments.add("-Dproject.settings=" + sonarProjectPropertiesPath);
            }

            // Add all arguments to the process
            processBuilder.command(processBuilderArguments);

            // To redirect output of the scanning process to the initiated console
            processBuilder.inheritIO();

            // Trigger the reporting process
            try {
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode == 0) {
                    outputStream.println("Reporting successful!");
                } else {
                    outputStream.println("Reporting failed!");
                }
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void saveIssues(String fileName, List<Issue> issues) {
        // Convert the output to a string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        String jsonOutput = gson.toJson(issuesAsJson);

        // Save analysis results to file
        File destination = new File(fileName);
        try (FileWriter writer = new FileWriter(destination, StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
