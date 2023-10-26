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
        // TODO: Set a method to get a files absolute path through the Sonar Plugin API's InputFile feature
        // TODO: After the ballerina tool is created setup a way to pass the File Absolute path to trigger it
        // TODO: Next if there are any rule violations detected by the ballerina tool get the outputs of them
        // TODO: Report the rule violations back to SonarQube by attaching the outputs of the bal tool

        // NOTE: On setting the ballerina.home property
        // Setting up the ballerina home property from the sensor side depending on the type of OS the plugin is
        // running on (In the go plugin it has been done from the Java side as well)
        // the set bal home is not neccessary as it's already using the users ballerina distribution
        // so the below function should not be unneccessary
        setBalHome();

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

            // create ana analyzer class in java, and inside that you can get the semantic model and visit
            // we can call the analyzer through groovy side
            // Analyzer will
            //
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
    }
}