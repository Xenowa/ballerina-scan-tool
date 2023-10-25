package org.wso2.ballerina.plugin;

// Ballerina specific imports
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;

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
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

// Other imports
import org.wso2.ballerina.checks.FunctionChecks;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

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

    public void setBalHome(){
        // Running bal home command depending on the os
        String command = System.getProperty("os.name").startsWith("Windows")
                ? "cmd /c bal home"
                : "sh -c bal home";

        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // getting the result from the executed command
        ByteArrayOutputStream balHome = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while (true) {
            try {
                if ((length = process.getInputStream().read(buffer)) == -1) break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            balHome.write(buffer, 0, length);
        }

        // Setting the root path to the distribution obtained by bal home command
        System.setProperty("ballerina.home", balHome.toString().trim());
    }

    // The place which the entire scan logic should be defined, this is the starting point of the scanner
    @Override
    public void execute(SensorContext sensorContext) {
        // Setting up the ballerina home property from the sensor side depending on the type of OS the plugin is
        // running on (In the go plugin it has been done from the Java side as well)
        setBalHome();

        // TODO: Implement feature to identify only ballerina files and create an Sonar InputFile using the semantic model
        // Retrieve all .bal source files from a project
        FileSystem fileSystem = sensorContext.fileSystem();
        FilePredicate mainFilePredicate = fileSystem.predicates()
                .and(
                        fileSystem.predicates().hasLanguage(language.getKey()),
                        fileSystem.predicates().hasType(InputFile.Type.MAIN)
                );
        Iterable<InputFile> filesToAnalyze = fileSystem.inputFiles(mainFilePredicate);

        for(InputFile inputFile : filesToAnalyze){
            // ==================================
            // Ballerina Specific Implementations
            // ==================================
            // Ballerina Specific implementations start from here:
            Path filePath = inputFile.path();

            try {
                // Load the Ballerina file
                Project project = ProjectLoader.loadProject(filePath);

                // get the document ID by considering if the project structure is relevant to Ballerina
                DocumentId documentId = project.documentId(filePath);
                if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                    documentId = project.documentId(filePath);
                } else {
                    // If project structure is different go to the next document
                    Module currentModule = project.currentPackage().getDefaultModule();
                    Iterator<DocumentId> documentIterator = currentModule.documentIds().iterator();

                    // block is used to prevent crashing
                    try{
                        documentId = documentIterator.next();
                    }catch (NoSuchElementException exception){
                        LOG.error(exception.toString());
                    }
                }

                // Compile the Ballerina source code file
                PackageCompilation compilation = project.currentPackage().getCompilation();
                // SemanticModel semanticModel = compilation.getSemanticModel(documentId.moduleId());
                // List<Symbol> symbols = semanticModel.visibleSymbols();


                // Retrieve the BLangPackage Node
                BLangPackage bLangPackage = compilation.defaultModuleBLangPackage();

                // Start performing checks
                FunctionChecks.tooManyParametersCheck(sensorContext, inputFile, bLangPackage);
            }catch (Exception exception){
                LOG.error(exception.toString());
            }
        }

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