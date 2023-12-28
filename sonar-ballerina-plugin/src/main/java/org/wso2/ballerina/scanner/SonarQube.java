package org.wso2.ballerina.scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.apache.commons.lang3.SystemUtils;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.PlatformPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SonarQube extends PlatformPlugin {
    private List<String> arguments;
    private ProcessBuilder processBuilder;
    private Map<String, String> platformArgs;

    @Override
    public String initialize(Map<String, String> platformArgs) {
        this.platformArgs = platformArgs;

        arguments = new ArrayList<>();

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

        // Initialize the process builder
        processBuilder = new ProcessBuilder();

        return "SonarQube";
    }

    @Override
    public void onScan(ArrayList<Issue> issues) {
        // If file path is provided save issues to it else save it to a default file
        String analyzedResultsPath = platformArgs.getOrDefault("analyzedResultsPath", null);

        if (analyzedResultsPath != null) {
            String analyzedReportPath = saveIssues(issues, analyzedResultsPath);
            System.out.println("Issues saved at: " + analyzedReportPath);
        } else {
            String analyzedReportPath = saveIssues(issues, "ballerina-analysis-results.json");

            // Check if there is a results file and add to properties if it exists
            if (analyzedReportPath != null) {
                arguments.add("-DanalyzedResultsPath=" + analyzedReportPath);
            }

            // Add all arguments to the process
            processBuilder.command(arguments);

            // To redirect output of the scanning process to the initiated console
            processBuilder.inheritIO();

            // Trigger the reporting process
            try {
                Process process = processBuilder.start();
                InputStream inputStream = process.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String saveIssues(ArrayList<Issue> issues, String fileName) {
        // Convert the output to a string
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        String jsonOutput = gson.toJson(issuesAsJson);

        // Save analysis results to file
        File newTempFile;
        try {
            newTempFile = new File(fileName);

            // Create a new file to hold analysis results
            newTempFile.createNewFile();

            // write the analysis results to the new file
            FileWriter writer = new FileWriter(newTempFile);
            writer.write(jsonOutput);
            writer.close();

        } catch (IOException e) {
            return null;
        }

        return newTempFile.getAbsolutePath();
    }
}