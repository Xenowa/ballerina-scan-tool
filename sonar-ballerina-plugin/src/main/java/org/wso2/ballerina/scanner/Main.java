package org.wso2.ballerina.scanner;

import org.apache.commons.lang3.SystemUtils;
import org.sonarsource.scanner.api.EmbeddedScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Main {
    // Initiating sonar-scanner through the plugin
    // =========
    // New Logic
    // =========
    // TODO: Implement a custom sonar scanner for ballerina to be triggered by the plugin itself
    public static void main(String[] args) {
        // Initializing the required attributes
        Logs logs = new Logs(System.out, System.err);
        Cli cli = new Cli(logs).parse();
        Conf conf = new Conf(cli, logs, System.getenv());
        ScannerFactory scannerFactory = new ScannerFactory(logs);
        EmbeddedScanner embeddedScanner;

        // Performing the custom sonar scan
        Properties p = conf.properties();

        // By default, the sonar scan executed through bal scan tool will execute ballerina files
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

        embeddedScanner = scannerFactory.create(p, "");

        // initialize the project
        embeddedScanner.start();

        // trigger the plugins and analysis
        embeddedScanner.execute((Map) p);
    }

    // =========
    // Old Logic
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
