package org.wso2.ballerina.scanner;

import org.apache.commons.lang3.SystemUtils;
import org.wso2.ballerina.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SonarQube extends Platform {
    @Override
    public String initialize() {
        return "SonarQube";
    }

    // =========================================
    // Method 1 (Decoupled Static Code Analysis)
    // =========================================
    @Override
    public void onScan(String analyzedReportPath, PrintStream outputStream, PrintStream errorStream) {
        triggerSonarScan(analyzedReportPath, outputStream);
    }

    public void triggerSonarScan(String resultsFilePath, PrintStream outputStream) {
        // Executing sonar-scanner cli through process builder
        List<String> arguments = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
        } else {
            arguments.add("sh");
            arguments.add("-c");
        }
        arguments.add("sonar-scanner");

        // Check if there is a results file and add to properties if it exists
        if (resultsFilePath != null) {
            arguments.add("-DanalyzedResultsPath=" + resultsFilePath);
        }

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

        // Execute the sonar-scanner through a sub, sub process
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
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