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

package io.ballerina.sonar;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.SystemUtils;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.ballerina.sonar.BallerinaPlugin.BALLERINA_REPOSITORY_KEY;

class BallerinaSensor implements Sensor {

    private static final String TARGET_FOLDER = "target";
    private static final String REPORT_FOLDER = "report";
    private static final String ANALYZED_RESULTS_FILE = "scan_results.json";
    private static final Logger LOG = Loggers.get(BallerinaSensor.class);
    private final FileLinesContextFactory fileLinesContextFactory;
    private final NoSonarFilter noSonarFilter;
    private final BallerinaLanguage language;
    private final CheckFactory checkFactory;

    private final ArrayList<String> externalRules = new ArrayList<>();

    // Initialize language specific information when the plugin is triggered
    public BallerinaSensor(CheckFactory checkFactory,
                           FileLinesContextFactory fileLinesContextFactory,
                           NoSonarFilter noSonarFilter,
                           BallerinaLanguage language) {

        this.checkFactory = checkFactory;
        this.fileLinesContextFactory = fileLinesContextFactory;
        this.noSonarFilter = noSonarFilter;
        this.language = language;
    }

    // Method which defines which language files the plugin should work with
    @Override
    public void describe(SensorDescriptor descriptor) {

        descriptor
                .onlyOnLanguage(language.getKey())
                .name(language.getName() + " Sensor");
    }

    // The place which the entire scan logic should be defined, this is the starting point of the scanner
    @Override
    public void execute(SensorContext sensorContext) {
        // Separate the InputFile components to a Map
        FileSystem fileSystem = sensorContext.fileSystem();
        FilePredicate mainFilePredicate = sensorContext.fileSystem().predicates()
                .and(
                        fileSystem.predicates().hasLanguage(language.getKey()),
                        fileSystem.predicates().hasType(InputFile.Type.MAIN)
                );
        Map<String, InputFile> pathAndInputFiles = new HashMap<>();
        fileSystem.inputFiles(mainFilePredicate).forEach(inputFile -> {
            pathAndInputFiles.put(inputFile.path().toString(), inputFile);
        });

        // Check if a scanned files report is present
        String analyzedResultsPath = sensorContext.config().get("analyzedResultsPath").orElse(null);
        if (analyzedResultsPath != null) {
            // Triggered through bal scan --platform=sonarqube
            processAnalyzedResultsReport(sensorContext, pathAndInputFiles, analyzedResultsPath);
        } else {
            // Triggered through sonar-scanner
            performLibraryCall(sensorContext, pathAndInputFiles);
        }
    }

    public void processAnalyzedResultsReport(SensorContext context,
                                             Map<String, InputFile> pathAndInputFiles,
                                             String analyzedResultsFilePath) {

        LOG.info("Analyzing batch report: ", analyzedResultsFilePath);
        String fileContent = getFileContent(analyzedResultsFilePath);

        reportFileContent(context, pathAndInputFiles, fileContent);
    }

    public void performLibraryCall(SensorContext context, Map<String, InputFile> pathAndInputFiles) {

        LOG.info("Analyzing Ballerina project");

        ProcessBuilder fileScan = new ProcessBuilder();

        fileScan.directory(context.fileSystem().baseDir());

        List<String> arguments = new ArrayList<>();

        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
        } else {
            arguments.add("sh");
            arguments.add("-c");
        }

        arguments.add("bal");
        arguments.add("scan");
        arguments.add("--quiet");

        fileScan.command(arguments);

        try {
            // To redirect output of the scanning process to the initiated console
            fileScan.inheritIO();

            // Start the process
            Process process = fileScan.start();

            // Wait for results file creation
            int exitCode = process.waitFor();

            // If file creation was successful proceed reporting
            if (exitCode == 0) {
                String analyzedResultsFilePath = Paths.get(context.fileSystem().baseDir().getPath())
                        .resolve(TARGET_FOLDER)
                        .resolve(REPORT_FOLDER)
                        .resolve(ANALYZED_RESULTS_FILE)
                        .toString();
                String fileContent = getFileContent(analyzedResultsFilePath);
                reportFileContent(context, pathAndInputFiles, fileContent);
            } else {
                LOG.info("Unable to analyze ballerina file batch!");
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private String getFileContent(String analyzedResultsFilePath) {

        String fileContent = "";
        // Read the file
        try (
                FileReader fileReader = new FileReader(analyzedResultsFilePath, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(fileReader)
        ) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fileContent = stringBuilder.toString();
        } catch (IOException e) {
            LOG.info("Unable to retrieve analysis results!");
        }
        return fileContent;
    }

    private void reportFileContent(SensorContext context,
                                   Map<String, InputFile> pathAndInputFiles,
                                   String fileContent) {
        // Parse the string into a JsonArray
        JsonArray balScanOutput = null;
        try {
            balScanOutput = JsonParser.parseString(fileContent).getAsJsonArray();
        } catch (Exception ignored) {
            LOG.info("Unable to report analysis results!");
        }

        // Report parsed issues
        boolean reportingSuccessful = reportAnalysisIssues(context, balScanOutput, pathAndInputFiles);
        if (reportingSuccessful) {
            LOG.info("Ballerina analysis successful!");
        } else {
            LOG.info("Unable to analyze ballerina file batch!");
        }
    }

    public boolean reportAnalysisIssues(SensorContext context,
                                        JsonArray analysisIssues,
                                        Map<String, InputFile> pathAndInputFiles) {

        boolean reportingSuccessful = false;
        // Perform the remaining operations if the output is not empty
        if (analysisIssues != null) {
            // Iteratively report issues
            for (JsonElement issueElement : analysisIssues) {
                // first convert the element into an object
                JsonObject issue = issueElement.getAsJsonObject();

                // Retrieve the absolute path of the scanned ballerina file
                String absoluteFilePath = issue.get("reportedFilePath").getAsString();

                // retrieve the InputFile relevant to the path of the analyzed file
                InputFile inputFile = pathAndInputFiles.get(absoluteFilePath);

                // Get the issue type from the output
                String issueType = issue.get("issueType").getAsString();

                // Perform validations on the issueType and proceed
                switch (issueType) {
                    case "CHECK_VIOLATION" -> reportIssue(inputFile, context, issue);
                    case "CUSTOM_CHECK_VIOLATION" -> reportExternalIssue(inputFile, context, issue);
                    default -> LOG.info("Invalid Issue Format!");
                }
            }

            reportingSuccessful = true;
        }

        return reportingSuccessful;
    }

    public void reportIssue(InputFile inputFile, SensorContext context, JsonObject balScanOutput) {
        // parsing JSON issue outputs to the formats required to report via the Sonar Scanner
        String ruleID = balScanOutput.get("ruleID").getAsString();
        String message = balScanOutput.get("message").getAsString();
        int startLine = balScanOutput.get("startLine").getAsInt();
        int startLineOffset = balScanOutput.get("startLineOffset").getAsInt();
        int endLine = balScanOutput.get("endLine").getAsInt();
        int endLineOffset = balScanOutput.get("endLineOffset").getAsInt();
        // It's required to add the offset here as in Ballerina the start position starts from 0 but in here it starts
        // from 1
        int sonarScannerOffset = 1;

        // Creating the initial rule
        RuleKey ruleKey = RuleKey.of(BALLERINA_REPOSITORY_KEY, ruleID);

        // reporting the issue to SonarQube
        context.newIssue()
                .forRule(ruleKey)
                .at(context.newIssue()
                        .newLocation()
                        .on(inputFile)
                        .message(message)
                        .at(inputFile.newRange(
                                startLine + sonarScannerOffset,
                                startLineOffset,
                                endLine + sonarScannerOffset,
                                endLineOffset
                        ))
                )
                .save();
    }

    public void reportExternalIssue(InputFile inputFile, SensorContext context, JsonObject balScanOutput) {
        // parsing JSON issue outputs to the formats required to report via the Sonar Scanner
        String ruleID = balScanOutput.get("ruleID").getAsString();
        String message = balScanOutput.get("message").getAsString();
        int startLine = balScanOutput.get("startLine").getAsInt();
        int startLineOffset = balScanOutput.get("startLineOffset").getAsInt();
        int endLine = balScanOutput.get("endLine").getAsInt();
        int endLineOffset = balScanOutput.get("endLineOffset").getAsInt();
        // It's required to add the offset here as in Ballerina the start position starts from 0 but in here it starts
        // from 1
        int sonarScannerOffset = 1;

        // Checking if the ruleID already exists in the externalIssues, if not create a new adhoc issue
        if (!externalRules.contains(ruleID)) {
            /**
             * Create a new ad hoc rule
             * {@link org.sonar.api.batch.sensor.rule.internal.DefaultAdHocRule}
             * */
            context.newAdHocRule()
                    .engineId("bal_scan_tool")
                    .ruleId(ruleID)
                    .name("Custom_Rule " + ruleID)
                    .type(RuleType.CODE_SMELL)
                    .severity(Severity.MAJOR)
                    .description(message)
                    .save();

            // Add the new ruleID to external rules arraylist
            externalRules.add(ruleID);
        }

        // Report the custom rule as an external issue
        /**
         * {@link org.sonar.api.batch.sensor.issue.internal.DefaultExternalIssue}
         * */
        context.newExternalIssue()
                .engineId("bal_scan_tool")
                .ruleId(ruleID)
                .type(RuleType.CODE_SMELL)
                .severity(Severity.MAJOR)
                .remediationEffortMinutes(10L)
                .at(context.newIssue()
                        .newLocation()
                        .on(inputFile)
                        .message(message)
                        .at(inputFile.newRange(
                                startLine + sonarScannerOffset,
                                startLineOffset,
                                endLine + sonarScannerOffset,
                                endLineOffset
                        ))
                )
                .save();
    }
}
