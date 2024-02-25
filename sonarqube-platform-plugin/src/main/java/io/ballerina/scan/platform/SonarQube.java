package io.ballerina.scan.platform;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.scan.Issue;
import io.ballerina.scan.ScannerPlatformPlugin;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SonarQube implements ScannerPlatformPlugin {

    private final List<String> arguments = new ArrayList<>();
    private final ProcessBuilder processBuilder = new ProcessBuilder();
    private final Map<String, String> platformArgs = new HashMap<>();
    private final PrintStream outputStream = System.out;
    private static final String ISSUES_FILE_PATH = "ballerina-analysis-results.json";

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

        boolean issuesSaved = saveIssues(ISSUES_FILE_PATH, issues);

        if (issuesSaved) {
            arguments.add("-DanalyzedResultsPath=" + Path.of(ISSUES_FILE_PATH).toAbsolutePath());

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
        } else {
            outputStream.println("Unable to save issues to file!");
        }
    }

    private boolean saveIssues(String fileName, ArrayList<Issue> issues) {
        // Convert the output to a string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        String jsonOutput = gson.toJson(issuesAsJson);

        // Save analysis results to file
        File destination = new File(fileName);
        try (FileWriter writer = new FileWriter(destination, StandardCharsets.UTF_8)) {
            writer.write(jsonOutput);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
