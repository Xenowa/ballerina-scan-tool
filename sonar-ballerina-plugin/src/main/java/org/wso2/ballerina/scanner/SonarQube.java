package org.wso2.ballerina.scanner;

import org.apache.commons.lang3.SystemUtils;
import org.wso2.ballerina.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SonarQube extends Platform {
    private List<String> arguments;
    private ProcessBuilder processBuilder;

    @Override
    public String initialize() {
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

    // =========================================
    // Method 1 (Decoupled Static Code Analysis)
    // =========================================
    // TODO: The report generation should happen here with the array of issues passed from the Ballerina scan tool
    @Override
    public void onScan(String analyzedReportPath, PrintStream outputStream, PrintStream errorStream) {
        // Check if there is a results file and add to properties if it exists
        if (analyzedReportPath != null) {
            arguments.add("-DanalyzedResultsPath=" + analyzedReportPath);
        }

        // Add all arguments to the process
        processBuilder.command(arguments);

        // Trigger the reporting process
        try {
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outputStream.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}