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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.wso2.ballerina.scanner.Main;

import static org.wso2.ballerina.plugin.BallerinaPlugin.BALLERINA_REPOSITORY_KEY;

class BallerinaSensor implements Sensor {
    private static final Logger LOG = Loggers.get(BallerinaSensor.class);
    private final FileLinesContextFactory fileLinesContextFactory;
    private final NoSonarFilter noSonarFilter;
    private final BallerinaLanguage language;
    private final CheckFactory checkFactory;

    private final ArrayList<String> externalRules = new ArrayList<>();

    // Initialize language specific information when the plugin is triggered
    public BallerinaSensor(CheckFactory checkFactory, FileLinesContextFactory fileLinesContextFactory, NoSonarFilter noSonarFilter, BallerinaLanguage language) {
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
        // Retrieve all .bal source files from a project
        FileSystem fileSystem = sensorContext.fileSystem();
        FilePredicate mainFilePredicate = fileSystem.predicates()
                .and(
                        fileSystem.predicates().hasLanguage(language.getKey()),
                        fileSystem.predicates().hasType(InputFile.Type.MAIN)
                );
        Iterable<InputFile> filesToAnalyze = fileSystem.inputFiles(mainFilePredicate);

        // Iterate through all ballerina files and perform analysis
        for (InputFile inputFile : filesToAnalyze) {
            analyzeFile(inputFile, sensorContext);
        }
    }


    // Create a common object and pass between both by casting to required format
    public void performScan(SensorContext sensorContext) {
        // Check where the scanner was triggered and accordingly perform the scan:
        // -Dscanner=ballerina
        String scannerName = sensorContext.config().get("scannerName").orElse(null);
        if (scannerName.equals("ballerina")) {
            FilePredicate mainFilePredicate = sensorContext.fileSystem().predicates()
                    .and(
                            sensorContext.fileSystem().predicates().hasLanguage(language.getKey()),
                            sensorContext.fileSystem().predicates().hasType(InputFile.Type.MAIN)
                    );

            // Since we will be iterating through each input file more than once during the scan we are placing it to an arraylist
            ArrayList<InputFile> inputFiles = new ArrayList<>();
            sensorContext.fileSystem().inputFiles(mainFilePredicate).forEach(inputFiles::add);

            // Iterate through all files and receive unique folder paths
            Set<Path> balFolderPaths = new HashSet<>();
            for (InputFile inputFile : inputFiles) {
                balFolderPaths.add(inputFile.path().getParent());
            }

            // TODO: Check the method to do this:
            //  - make the call to the onScan() method through here
            // call the onScan method in the bal scan tool from the child and retrieve the scanned results
            // JsonArray analyzedFiles = parentProcess.onScan(balFolderPaths);
            JsonArray analyzedFiles = new JsonArray();

            // Iterate through each file object in the array
            analyzedFiles.forEach(analyzedFile -> {
                // Retrieve it as a JSON Object
                JsonObject reportedFile = analyzedFile.getAsJsonObject();

                // Retrieve the absolute path of the file
                String absolutePathOfReportedFile = reportedFile.get("ballerinaFilePath").getAsString();

                // Retrieve the reported issues relevant to the file
                JsonArray reportedIssues = reportedFile.getAsJsonArray("reportedIssues");

                // Iterate through each issue object
                reportedIssues.forEach(issue -> {
                    // Retrieve the issue as a jsonObject
                    JsonObject issueObject = issue.getAsJsonObject();

                    // report the issue
                    newreportIssue(inputFiles, sensorContext, absolutePathOfReportedFile, issueObject);
                });

            });
        } else {
            // Trigger the bal scanner

        }
    }

    public void newreportIssue(ArrayList<InputFile> inputFiles, SensorContext context, String absoluteFilePath, JsonObject issue) {
        // Retrieve the correct InputFile based on the absolute path of the issue and the absolute path of the inputfile
        AtomicReference<InputFile> matchingInputFile = null;
        inputFiles.forEach(inputFile -> {
            if (inputFile.absolutePath().equals(absoluteFilePath)) {
                matchingInputFile.set(inputFile);
            }
        });

        // Only perform reporting if the file actually exist
        if (matchingInputFile.get() != null) {
            // parsing JSON issue outputs to the formats required to report via the Sonar Scanner
            String ruleID = issue.get("ruleID").getAsString();
            String message = issue.get("message").getAsString();
            int startLine = issue.get("startLine").getAsInt();
            int startLineOffset = issue.get("startLineOffset").getAsInt();
            int endLine = issue.get("endLine").getAsInt();
            int endLineOffset = issue.get("endLineOffset").getAsInt();
            // It's required to add the offset here as in Ballerina the start position starts from 0 but in here it starts from 1
            int sonarScannerOffset = 1;

            // Creating the initial rule
            RuleKey ruleKey = RuleKey.of(BALLERINA_REPOSITORY_KEY, ruleID);

            // reporting the issue to SonarQube
            context.newIssue()
                    .forRule(ruleKey)
                    .at(context.newIssue()
                            .newLocation()
                            .on(matchingInputFile.get())
                            .message(message)
                            .at(matchingInputFile.get().newRange(
                                    startLine + sonarScannerOffset,
                                    startLineOffset,
                                    endLine + +sonarScannerOffset,
                                    endLineOffset
                            ))
                    )
                    .save();
        }
    }

    public void analyzeFile(InputFile inputFile, SensorContext context) {
        String absolutePath = inputFile.path().toAbsolutePath().toString();
        LOG.info("analyzing File: " + absolutePath);

        // Build a process to run the bal tool depending on user rule inputs
        String userRule = context.config().get("rule").orElse(null);
        ProcessBuilder fileScan;
        if (userRule != null) {
            fileScan = new ProcessBuilder("cmd", "/c", "bal", "scan", absolutePath, "--rule=" + userRule);
        } else {
            fileScan = new ProcessBuilder("cmd", "/c", "bal", "scan", absolutePath);
        }

        try {
            // Start the process
            Process process = fileScan.start();

            // Read the output of the process into a string
            InputStream scanProcessInput = process.getInputStream();
            Scanner scanner = new Scanner(scanProcessInput).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";

            JsonArray balScanOutput;
            try {
                // Parse the object into a JSON object
                balScanOutput = JsonParser.parseString(output).getAsJsonArray();

                // Perform the remaining operations if the output is not empty
                if (!balScanOutput.isEmpty()) {
                    // Iteratively perform per file reporting
                    for (JsonElement scannedFileElement : balScanOutput) {
                        // first convert the element into an object
                        JsonObject analyzedFile = scannedFileElement.getAsJsonObject();

                        // TODO: Retrieve the absolute path of the scanned file (required for determining the correct
                        //  InputFile component before reporting)
                        String absoluteFilePath = analyzedFile.get("ballerinaFilePath").getAsString();

                        // perform the remaining operations if the output is not empty
                        JsonArray issues = analyzedFile.get("reportedIssues").getAsJsonArray();
                        if (!issues.isEmpty()) {
                            // Iteratively perform reporting from SonarScanner
                            for (JsonElement scannedIssueElement : issues) {
                                // first convert the element into an object
                                JsonObject issue = scannedIssueElement.getAsJsonObject();

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
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reportIssue(InputFile inputFile, SensorContext context, JsonObject balScanOutput) {
        // parsing JSON issue outputs to the formats required to report via the Sonar Scanner
        String ruleID = balScanOutput.get("ruleID").getAsString();
        String message = balScanOutput.get("message").getAsString();
        int startLine = balScanOutput.get("startLine").getAsInt();
        int startLineOffset = balScanOutput.get("startLineOffset").getAsInt();
        int endLine = balScanOutput.get("endLine").getAsInt();
        int endLineOffset = balScanOutput.get("endLineOffset").getAsInt();
        // It's required to add the offset here as in Ballerina the start position starts from 0 but in here it starts from 1
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
        // It's required to add the offset here as in Ballerina the start position starts from 0 but in here it starts from 1
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