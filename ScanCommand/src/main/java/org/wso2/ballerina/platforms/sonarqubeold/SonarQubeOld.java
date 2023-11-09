package org.wso2.ballerina.platforms.sonarqubeold;

import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.sonarsource.scanner.api.EmbeddedScanner;
import org.wso2.ballerina.checks.functionChecks.FunctionChecks;
import org.wso2.ballerina.platforms.Platform;

import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;

public class SonarQubeOld extends Platform {
    // Final attributes for determining the type of issues reported to Sonar Scanner
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    public void scan(String userFile, PrintStream outputStream) {
        // initiating a scan from the bal scan tool to perform analysis in a project
        PluginTrigger.triggerPlugin();

        // Integrating the static code analysis from the bal tool with the triggered scan
//        // parse the ballerina file
//        Map<String, Object> compilation = parseBallerinaProject(userFile);
//
//        // perform the static code analysis if the file was successfully parsed
//        if(compilation != null){
//            scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"));
//
//            // The semantic model will be used later when implementing complex rules
//            // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);
//        }
//
//        // Convert the JSON analysis results to the console
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        String jsonOutput = gson.toJson(analysisIssues);
//
//        // Identifying if the sonar ballerina jar file is installed in sonarqube 9.9
//        // If it's installed then calling that jar file from here
//
//        outputStream.println(jsonOutput);
    }

    public static class PluginTrigger {
        // Properties related to initiating a sonar scan from bal scan side
        private Conf conf;
        private Cli cli;
        private ScannerFactory runnerFactory;
        private Logs logger;
        private EmbeddedScanner embeddedScanner;

        PluginTrigger(Cli cli, Conf conf, ScannerFactory runnerFactory, Logs logger) {
            this.cli = cli;
            this.conf = conf;
            this.runnerFactory = runnerFactory;
            this.logger = logger;
        }

        public static void triggerPlugin() {
            Logs logs = new Logs(System.out, System.err);
            Cli cli = new Cli(logs).parse();
            PluginTrigger pluginTrigger = new PluginTrigger(cli, new Conf(cli, logs, System.getenv()), new ScannerFactory(logs), logs);
            pluginTrigger.execute();
        }

        public void execute() {
            try {
                Properties p = conf.properties();
                init(p);

                // ==================================================================
                // Replacing the CustomToolClassLoader with a new Custom Class Loader
                // ==================================================================
                // Get the current context of the class loader
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();

                // Create and set the new custom class loader to perform operations
                CustomBalScanClassLoader customBalScanClassLoader =
                        new CustomBalScanClassLoader("C:/Users/Tharana Wanigaratne/.ballerina/repositories/central.ballerina.io/bala/tharana_wanigaratne/tool_scan/0.1.0/java17/tool/libs/ScanCommand-all.jar"
                                , "sonar-scanner-api-batch.jar");
                Thread.currentThread().setContextClassLoader(customBalScanClassLoader);

                embeddedScanner.start();

                // Replace the custom class loader with the previous class loader
                Thread.currentThread().setContextClassLoader(oldClassLoader);

                execute(p);
            } catch (Throwable e) {
                logger.info("Error during SonarScanner execution");
            }
        }

        private void init(Properties p) {
            embeddedScanner = runnerFactory.create(p, "");
        }

        private void execute(Properties p) {
            embeddedScanner.execute((Map) p);
        }
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree) {
        // Function related visits
        FunctionChecks functionChecks = new FunctionChecks(syntaxTree);
        functionChecks.initialize();

        // Other visits
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
    }

    public void handleParseIssue(String userFile) {
        JsonObject jsonObject = new JsonObject();

        // Create a JSON Object of the error
        jsonObject.addProperty("issueType", SOURCE_INVALID);
        String message = "Unable to parse file " + userFile;
        jsonObject.addProperty("message", message);

        // add the analysis issue to the issues array
        analysisIssues.add(jsonObject);
    }
}
