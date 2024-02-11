package org.wso2.ballerina.plugin;

// Sonar Plugin API imports

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

// Other imports
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.wso2.ballerina.plugin.BallerinaPlugin.BALLERINA_REPOSITORY_KEY;

class BallerinaSensor implements Sensor {
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
        String analyzedResultsFilePath = "ballerina-analysis-results.json";
        ProcessBuilder fileScan = new ProcessBuilder("cmd",
                "/c",
                "bal",
                "scan",
                "--platforms=sonarqube",
                "-PARG=analyzedResultsFilePath=" + analyzedResultsFilePath);

        try {
            // Start the process
            Process process = fileScan.start();

            // Wait for results file creation
            int exitCode = process.waitFor();

            // If file creation was successful proceed reporting
            if (exitCode == 0) {
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
        try {
            // Read the file using FileReader and BufferedReader
            FileReader fileReader = new FileReader(analyzedResultsFilePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            fileContent = stringBuilder.toString();

            // Close the readers
            bufferedReader.close();
            fileReader.close();
        } catch (Exception ignored) {
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
                    case "CHECK_VIOLATION":
                        reportIssue(inputFile, context, issue);
                        break;
                    case "CUSTOM_CHECK_VIOLATION":
                        reportExternalIssue(inputFile, context, issue);
                        break;
                    case "SOURCE_INVALID":
                        reportParseIssue(issue.get("message").getAsString());
                        break;
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
                                endLine + +sonarScannerOffset,
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
            // Create a new ad hoc rule
            /**
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

        // Report the custom rule as a external issue
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
                                endLine + +sonarScannerOffset,
                                endLineOffset
                        ))
                )
                .save();
    }

    public void reportParseIssue(String message) {
        LOG.error(message);
    }
}