package org.wso2.ballerina.plugin;

// Sonar Plugin API imports
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.issue.NoSonarFilter;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

// Ballerina specific imports
import static org.wso2.ballerina.plugin.BallerinaPlugin.BALLERINA_REPOSITORY_KEY;

class BallerinaSensor implements Sensor {
    private static final Logger LOG = Loggers.get(BallerinaSensor.class);
    private final FileLinesContextFactory fileLinesContextFactory;
    private final NoSonarFilter noSonarFilter;
    private final BallerinaLanguage language;
    private final CheckFactory checkFactory;

    public BallerinaSensor(CheckFactory checkFactory,  FileLinesContextFactory fileLinesContextFactory,  NoSonarFilter noSonarFilter,  BallerinaLanguage language){
        // Initialize language specific information when the plugin is triggered
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
        // Providing the base minimum features required to report an issue to sonarqube once a scan is triggered
        // Dummy rule key creation
        RuleKey ruleKey = RuleKey.of(BALLERINA_REPOSITORY_KEY, "S107");

        // Feature to determine all input files
        // TODO: Implement feature to identify only ballerina files and create an Sonar InputFile using the semantic model
        // The sonar plugin API itself provides a way to determine the scanned files to be Ballerina files
        // It is implemented in all SonarQube plugins as follows:
        FileSystem fileSystem = sensorContext.fileSystem();
        FilePredicate mainFilePredicate = fileSystem.predicates()
                .and(
                        fileSystem.predicates().hasLanguage(language.getKey()),
                        fileSystem.predicates().hasType(InputFile.Type.MAIN)
                );

        // Setting up the files to be analyzed
        Iterable<InputFile> filesToAnalyze = fileSystem.inputFiles(mainFilePredicate);
        // It generates a list with the type InputFile
        // This is a requirement in order to perform reporting of broken rules to SonarQube

        // Reporting an issue for each input file scanned despite having no issues
        // Dummy message to be reported
        String message = "Reporting dummy Ballerina Check =)";
        for(InputFile inputFile : filesToAnalyze){
            // Dummy text range of the file being reported
            // This range is sensitive to the code provided and is aware regarding the starting point and ending points
            TextRange textRange = inputFile.newRange(
                    1,
                    0,
                    inputFile.lines(),
                    1
            );

            // Reporting the issue to SonarQube
            sensorContext.newIssue()
                    .forRule(ruleKey)
                    .at(sensorContext.newIssue()
                            .newLocation()
                            .on(inputFile)
                            .message(message)
                            .at(inputFile.newRange(textRange.start().line()
                                            , textRange.start().lineOffset()
                                            , textRange.end().line()
                                            , textRange.end().lineOffset()
                                    )
                            )
                    )
                    .save();
        }

        LOG.info("Ballerina Sensor execution completed!");

        // Starting the performance test of the sensor
//        PerformanceMeasure.Duration sensorDuration = createPerformanceMeasureReport(sensorContext);
//
//        // TODO: Implement feature to identify only ballerina files using the semantic model
//        // The sonar plugin API itself provides a way to determine the scanned files to be Ballerina files
//        // It is implemented in all SonarQube plugins as follows:
////        FileSystem fileSystem = sensorContext.fileSystem();
////        FilePredicate mainFilePredicate = fileSystem.predicates()
////                .and(
////                        fileSystem.predicates().hasLanguage(language.getKey()),
////                        fileSystem.predicates().hasType(InputFile.Type.MAIN)
////                );
////
////        // Setting up the files to be analyzed
////        Iterable<InputFile> filesToAnalyze = fileSystem.inputFiles(mainFilePredicate);
////        List<String> fileNames = new ArrayList<>();
////        for (InputFile fileToAnalyze : filesToAnalyze) {
////            fileNames.add(fileToAnalyze.toString());
////        }
//        // It generates a list with the type InputFile
//        // This is a requirement in order to perform reporting of broken rules to SonarQube
//
//        // Simulating a hardcoded dummy file
//        InputFileContext inputFileContext = new InputFileContext(sensorContext, null);
//        List<String> packageNames = new ArrayList<>();
//        packageNames.add("file1.bal");
//
//        // Start the sonar plugin API's visitor check duration tester
//        ProgressReport progressReport = new ProgressReport("Progress of the " + language.getName() + " analysis", TimeUnit.SECONDS.toMillis(10));
//
//        Boolean success = false;
//        try{
//            // TODO: Implement how ballerina can analyze packages using the semantic model
//            success = analyzePackages(sensorContext, progressReport, packageNames);
//        }finally {
//            if(success){
//                progressReport.stop();
//            }else {
//                progressReport.cancel();
//            }
//        }
//        sensorDuration.stop();
    }

//    // TODO: Implement how ballerina can analyze packages using the semantic model
//    private Boolean analyzePackages(SensorContext sensorContext, ProgressReport progressReport, List<String> packageNames){
//        progressReport.start(packageNames);
//
//        // TODO: Implement how Ballerina can provide the input file context for performing scans
//        // In the Kotlin plugin each file is provided with an input file context
//        InputFileContext inputFileContext = new InputFileContext(sensorContext, null);
//
//        // Place where analysis per each source file begins
//        measureDuration(""
//                , () -> {
//            return analyseFile(sensorContext, inputFileContext);
//        }
//        );
//
//        progressReport.nextFile();
//        return true;
//    }
//
//    // TODO: Implement how Ballerina can provide the visitors to scan the input file as well as pass the AST relevant for the scans
//    private <T> T analyseFile(SensorContext sensorContext, InputFileContext inputFileContext){
//        // Try catch is placed for the regular expression check, as Regex does not work with java 11,
//        // and we have to rely on the matcher method instead
//        try {
//            if(!EMPTY_FILE_CONTENT_PATTERN.matcher(inputFileContext.getInputFile().contents()).find()){
//                return null;
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        // TODO: Implement how Ballerina can provide the visitors to scan the input file as well as pass the AST relevant for the scans
//        visitFile(sensorContext, inputFileContext);
//        return null;
//    }
//
//    // TODO: Implement how Ballerina can provide the visitors to scan the input file as well as pass the AST relevant for the scans
//    private void visitFile(SensorContext sensorContext, InputFileContext inputFileContext) {
//        // For now we will always report a dummy issue for every file scanned
//        RuleKey dummyRuleKey = RuleKey.of("ballerina", "S107");
//        TextRange dummyTextRange = new TextRange() {
//            @Override
//            public TextPointer start() {
//                return null;
//            }
//
//            @Override
//            public TextPointer end() {
//                return null;
//            }
//
//            @Override
//            public boolean overlap(TextRange another) {
//                return false;
//            }
//        };
//        String dummyMessage = "Dummy rule of Ballerina lang has been violated! =)";
//
//        inputFileContext.reportIssue(dummyRuleKey, dummyTextRange, dummyMessage);
//    }
//
//    private void logParsingError(InputFile inputFile, ParseException e) {
//        TextPointer position = e.getPosition();
//        String positionMessage = "";
//        if(position != null){
//            positionMessage = "Parse error at position " + position.line() + ":" + position.lineOffset();
//        }
//        LOG.error("Unable to parse file: " + inputFile.uri() + "." + positionMessage);
//
//        if(e.getMessage() != null){
//            LOG.error(e.getMessage());
//        }
//    }
//
//    private ParseException toParseException(String action, InputFile inputFile, Throwable cause) {
//        return new ParseException("Cannot " + action + " " + inputFile.toString() + ": " + cause.getMessage()
//                , cause instanceof ParseException? ((ParseException) cause).getPosition() : null
//                , cause);
//    }

    // Sonar Plugin API Method to determine sensor execution performance
//    private PerformanceMeasure.Duration createPerformanceMeasureReport(SensorContext context){
//        return PerformanceMeasure.reportBuilder()
//                .activate(
//                        context
//                                .config()
//                                .get(PERFORMANCE_MEASURE_ACTIVATION_PROPERTY)
//                                .filter("true"::equals)
//                                .isPresent()
//                )
//                .toFile(context
//                        .config()
//                        .get(PERFORMANCE_MEASURE_DESTINATION_FILE)
//                        .orElse("/tmp/sonar.ballerina.performance.measure.json")
//                )
//                .appendMeasurementCost()
//                .start("BallerinaSensor");
//    }
}