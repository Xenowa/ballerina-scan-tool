package org.wso2.ballerina.internal.platforms;

import org.apache.commons.lang3.SystemUtils;
import org.wso2.ballerina.Platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.ballerina.internal.ScanCommand.userRule;

// TODO: Merge this implementation with the sonar-ballerina plugin
public class SonarQube extends Platform {
    @Override
    public void initialize() {
    }

    // =========================================
    // Method 1 (Decoupled Static Code Analysis)
    // =========================================
    @Override
    public void onScan(String scannedResults) {
        String resultsFilePath = saveResults(scannedResults);
        triggerSonarScan(resultsFilePath);
    }

    public String saveResults(String scannedResults) {
        // Save analysis results to file
        File newTempFile = null;
        try {
            newTempFile = new File("ballerina-analysis-results.json");

            // Create a new file to hold analysis results
            newTempFile.createNewFile();

            // write the analysis results to the new file
            FileWriter writer = new FileWriter(newTempFile);
            writer.write(scannedResults);
            writer.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return newTempFile.getAbsolutePath();
    }

    // ========================================
    // Method 2 (Embedded Static Code Analysis)
    // ========================================
    public void triggerSonarScan(String resultsFilePath) {
        // Call the sonar-scanner available in the sonar-ballerina plugin
        List<String> arguments = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
        } else {
            arguments.add("sh");
            arguments.add("-c");
        }

        // Mandatory arguments to execute the analysis
        arguments.add("java");
        arguments.add("-jar");
        arguments.add("C:/src/sonarqube-9.9.2.77730/extensions/plugins/sonar-ballerina-plugin-1.0-all.jar");

        // Add the argument to tell the sonar-scanner that an analysis results file has been created
        if (resultsFilePath != null) {
            arguments.add("analysisResults=" + resultsFilePath);
        }

        // if the user has passed the rule to be analyzed
        if (!userRule.equals("all")) {
            arguments.add("-Drule=" + userRule);
        }

        // Execute the sonar-scanner through a sub process
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
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