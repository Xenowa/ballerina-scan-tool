package org.wso2.ballerina.plugin;

// Ballerina specific imports
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Sonar Plugin API imports
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

// Other imports
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import static org.wso2.ballerina.plugin.BallerinaPlugin.BALLERINA_REPOSITORY_KEY;

class BallerinaSensor implements Sensor {
    private static final Logger LOG = Loggers.get(BallerinaSensor.class);
    private final FileLinesContextFactory fileLinesContextFactory;
    private final NoSonarFilter noSonarFilter;
    private final BallerinaLanguage language;
    private final CheckFactory checkFactory;

    // Initialize language specific information when the plugin is triggered
    public BallerinaSensor(CheckFactory checkFactory,  FileLinesContextFactory fileLinesContextFactory,  NoSonarFilter noSonarFilter,  BallerinaLanguage language){
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
        for(InputFile inputFile : filesToAnalyze){
            analyzeFile(inputFile, sensorContext);
        }
    }

    public void analyzeFile(InputFile inputFile, SensorContext context){
        String absolutePath = inputFile.path().toAbsolutePath().toString();
        LOG.info("analyzing File: " + absolutePath);

        // Build a process to run the bal tool
        ProcessBuilder fileScan = new ProcessBuilder("cmd","/c", "bal", "scan", "--platform=sonarqube", absolutePath);
        try {
            // Start the process
            Process process = fileScan.start();

            // Read the output of the process into a string
            InputStream scanProcessInput = process.getInputStream();
            Scanner scanner = new Scanner(scanProcessInput).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";

            JsonArray balScanOutput;
            try{
                // Parse the object into a JSON object
                 balScanOutput = JsonParser.parseString(output).getAsJsonArray();

                 // Perform the remaining operations if the output is not empty
                if(!balScanOutput.isEmpty()){
                    // Iteratively perform reporting from SonarScanner
                    for(JsonElement scanElement : balScanOutput){
                        // first convert the element into an object
                        JsonObject issue = scanElement.getAsJsonObject();

                        // Get the issue type from the output
                        String issueType = issue.get("issueType").getAsString();

                        // Perform validations on the issueType and proceed
                        switch (issueType){
                            case "CHECK_VIOLATION":
                                reportIssue(inputFile, context, issue);
                                break;
                            case "SOURCE_INVALID":
                                reportParseIssue(issue.get("message").getAsString());
                                break;
                        }
                    }
                }
            }catch (Exception ignored){}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reportIssue(InputFile inputFile, SensorContext context, JsonObject balScanOutput){
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
                                endLine+ + sonarScannerOffset,
                                endLineOffset
                        ))
                )
                .save();
    }

    public void reportParseIssue(String message){
        LOG.error(message);
    }
}