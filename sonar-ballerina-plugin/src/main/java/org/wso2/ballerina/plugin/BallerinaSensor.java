package org.wso2.ballerina.plugin;

// Sonar Plugin API imports

import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputDir;
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
import org.sonar.api.scanner.fs.InputProject;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

// Other imports
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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
        // TODO: initiate "bal scan --platform=sonarqube" through here:
        //  ===========================================================
        //  - The sensor context already has the absolute path of the working directory
        //  - Ideally this absolute path should be taken from the bal scan tool
        //  - Next this should be sent to the bal scan command to perform analysis
        //  - Next the analysis results, should be reported per file by matching the report absolute path
        //  retrieved against the absolute path of the ballerina file in the sensor context
        //  - It should be as follows:
        /**
         * TODO: bal scan triggering method:
         *  ================================
         *  FilePredicate mainFilePredicate = sensorContext.fileSystem().predicates()
         *         .and(
         *                 sensorContext.fileSystem().predicates().hasLanguage(language.getKey()),
         *                 sensorContext.fileSystem().predicates().hasType(InputFile.Type.MAIN)
         *         );
         *  *
         *  // Since we will be iterating through each input file more than once during the scan we are placing it to an arraylist
         *  ArrayList<InputFile> inputFiles = new ArrayList<>();
         *  sensorContext.fileSystem().inputFiles(mainFilePredicate).forEach(inputFiles::add);
         *  *
         *  // Iterating through all files and getting a list of all parent folder paths
         *  Map<InputFile,Path> balFileAndFolders = new HashMap<>();
         *  *
         *  for(InputFile inputFile : inputFiles){
         *        balFileAndFolders.put(inputFile, inputFile.path().getParent());
         *  }
         *  *
         *  // Perform the bal scan for each folder
         *  balFileAndFolders.forEach((inputFile, parentFolder) ->{
         *     // perform the bal scan for each
         *     ProcessBuilder balScan = new ProcessBuilder("cmd", "/c", "bal", "scan", parentFolder.toUri().toString());
         *     Process process = balScan.start();
         *     InputStream scanProcessInput = process.getInputStream();
         *     Scanner scanner = new Scanner(scanProcessInput).useDelimiter("\\A");
         *     String output = scanner.hasNext() ? scanner.next() : "";
         *  *
         *     // Only report issues of scans that were triggered successfully
         *     if(!output.equals("not a ballerina project")){
         *         // report the
         *         reportIssue(inputFile, sensorContext,new JsonObject());
         *     }
         *  });
         * */


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
                    // Iteratively perform reporting from SonarScanner
                    for (JsonElement scanElement : balScanOutput) {
                        // first convert the element into an object
                        JsonObject issue = scanElement.getAsJsonObject();

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