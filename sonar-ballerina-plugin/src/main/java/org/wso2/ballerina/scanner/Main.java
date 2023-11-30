package org.wso2.ballerina.scanner;

import org.sonarsource.scanner.api.LogOutput;
import org.sonarsource.scanner.api.internal.ClassloadRules;
import org.sonarsource.scanner.api.internal.IsolatedLauncherFactory;
import org.sonarsource.scanner.api.internal.batch.IsolatedLauncher;
import org.sonarsource.scanner.api.internal.cache.Logger;
import org.wso2.ballerina.Platform;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// main call should extend the abtract class AnalysisPlatform provided within the bal scan tool
public class Main {
    // Initiating sonar-scanner through the plugin
    // =========
    // New Logic
    // =========
    // This attempts to trigger the scan within the same process instead of having a seperate scaner process
    public static void main(String[] args) {
        // Initializing the required attributes
        Logs logs = new Logs(System.out, System.err);
        Cli cli = new Cli(logs).parse();
        Conf conf = new Conf(cli, logs, System.getenv());

        // Performing the custom sonar scan
        Properties p = conf.properties();

        // Set the property to identify the scan was performed using the ballerina scanner
        p.setProperty("sonar.scannerName", "ballerina");

        // Set the property to only scan ballerina files when the scan is triggered
        p.setProperty("sonar.exclusions", "'" +
                "**/*.java," +
                "**/*.xml," +
                "**/*.yaml," +
                "**/*.go," +
                "**/*.kt," +
                "**/*.js," +
                "**/*.html," +
                "**/*.YAML" +
                ",**/*.rb," +
                "**/*.scala," +
                "**/*.py" +
                "'");

        // ===============================================
        // Programmatically executing the embedded scanner
        // ===============================================
        // Creating a log output
        LogOutput logOutput = (formattedMessage, level) -> {
            switch (level) {
                case TRACE, DEBUG -> logs.debug(formattedMessage);
                case ERROR -> logs.error(formattedMessage);
                case WARN -> logs.warn(formattedMessage);
                default -> logs.info(formattedMessage);
            }
        };

        // Creating a logger to send messages to the console
        Logger logger = new Logger() {
            public void warn(String msg) {
                logOutput.log(msg, LogOutput.Level.WARN);
            }

            public void info(String msg) {
                logOutput.log(msg, LogOutput.Level.INFO);
            }

            public void error(String msg, Throwable t) {
                StringWriter errors = new StringWriter();
                t.printStackTrace(new PrintWriter(errors));
                logOutput.log(msg + "\n" + errors.toString(), LogOutput.Level.ERROR);
            }

            public void error(String msg) {
                logOutput.log(msg, LogOutput.Level.ERROR);
            }

            public void debug(String msg) {
                logOutput.log(msg, LogOutput.Level.DEBUG);
            }
        };

        // Creating a launcher that could classload the BatchIsolated launcher
        IsolatedLauncherFactory isolatedLauncherFactory = new IsolatedLauncherFactory(logger);

        // Creating a map to initialize project properties
        Map<String, String> globalProperties = new HashMap<>();

        globalProperties.putAll((Map) p);
        globalProperties.put("sonar.host.url", "http://localhost:9000");

        Set<String> classloaderMask = new HashSet<>();
        Set<String> classloaderUnMask = new HashSet<>();
        classloaderUnMask.add("org.sonarsource.scanner.api.internal.batch.");

        ClassloadRules rules = new ClassloadRules(classloaderMask, classloaderUnMask);
        IsolatedLauncher launcher = isolatedLauncherFactory.createLauncher(globalProperties, rules);

        // Triggering the execution of the sonar-batch-api through java reflection
        launcher.execute(globalProperties, (formattedMessage, level) -> {
            logOutput.log(formattedMessage, LogOutput.Level.valueOf(level.name()));
        });


        // =================
        // Original Approach (Direct utilization of Embedded scanner)
        // =================
//        ScannerFactory scannerFactory = new ScannerFactory(logs);
//        EmbeddedScanner embeddedScanner;
//        embeddedScanner = scannerFactory.create(p, "");
//
//        // initialize the project
//        // embeddedScanner.start();
//
//        // trigger the plugins and analysis
//        embeddedScanner.execute((Map) p);
    }

    // =========
    // Old Logic (Executing through sonar-scanner cli)
    // =========
//    public static void main(String[] args) {
//        PrintStream outputStream = new PrintStream(System.out);
//
//        List<String> arguments = new ArrayList<>();
//        if (SystemUtils.IS_OS_WINDOWS) {
//            arguments.add("cmd");
//            arguments.add("/c");
//        } else {
//            arguments.add("sh");
//            arguments.add("-c");
//        }
//        arguments.add("sonar-scanner");
//
//        // By default, the sonar scan executed through bal scan tool will execute ballerina files
//        arguments.add("-Dsonar.exclusions=" +
//                "'" +
//                "**/*.java," +
//                "**/*.xml," +
//                "**/*.yaml," +
//                "**/*.go," +
//                "**/*.kt," +
//                "**/*.js," +
//                "**/*.html," +
//                "**/*.YAML" +
//                ",**/*.rb," +
//                "**/*.scala," +
//                "**/*.py" +
//                "'");
//
//        if (args.length != 0) {
//            arguments.add(Arrays.toString(args));
//        }
//
//        // Execute the sonar-scanner through a sub, sub process
//        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
//        try {
//            Process process = processBuilder.start();
//            InputStream inputStream = process.getInputStream();
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                outputStream.println(line);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}
