package org.wso2.ballerina.plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.SystemUtils;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.ScannerPlatformPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SonarQube implements ScannerPlatformPlugin {

    private final List<String> arguments = new ArrayList<>();
    private final ProcessBuilder processBuilder = new ProcessBuilder();
    private final Map<String, String> platformArgs = new HashMap<>();
    private final PrintStream outputStream = System.out;

    @Override
    public String platformName() {

        return "sonarqube";
    }

    @Override
    public void initialize(Map<String, String> platformArgs) {

        this.platformArgs.putAll(platformArgs);

        // Initializing sonar-scanner cli
        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
        } else {
            arguments.add("sh");
            arguments.add("-c");
        }
        arguments.add("sonar-scanner");

        // Set the property to only scan ballerina files when the scan is triggered
        arguments.add("-Dsonar.exclusions=" +
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
    public void onScan(ArrayList<Issue> issues) {

        String analyzedReportPath = saveIssues(issues, "ballerina-analysis-results.json");
        if (analyzedReportPath != null) {
            arguments.add("-DanalyzedResultsPath=" + analyzedReportPath);
        }

        String sonarProjectPropertiesPath = platformArgs.getOrDefault("sonarProjectPropertiesPath",
                null);
        if (sonarProjectPropertiesPath != null) {
            arguments.add("-Dproject.settings=" + sonarProjectPropertiesPath);
        }

        // Add all arguments to the process
        processBuilder.command(arguments);

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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String saveIssues(ArrayList<Issue> issues, String fileName) {
        // Convert the output to a string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        String jsonOutput = gson.toJson(issuesAsJson);

        // Save analysis results to file
        File newTempFile = new File(fileName);
        boolean newFileCreated = false;
        try {
            newFileCreated = newTempFile.createNewFile();
        } catch (IOException e) {
            return null;
        }

        // write the analysis results to the new file
        if (newFileCreated) {
            try (FileWriter writer = new FileWriter(newTempFile, StandardCharsets.UTF_8)) {
                writer.write(jsonOutput);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return newTempFile.getAbsolutePath();
    }
}
