package org.wso2.ballerina.internal.platforms;

import org.apache.commons.lang3.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.ballerina.internal.ScanCommand.userRule;

public class SonarQube extends Platform {
    @Override
    public void scan(String userFile, PrintStream outputStream) {
    }

    // Temporary testing
    @Override
    public void scan(PrintStream outputStream) {
        List<String> arguments = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            arguments.add("cmd");
            arguments.add("/c");
        } else {
            arguments.add("sh");
            arguments.add("-c");
        }

        // Mandatory arguments to execute the analysis
        arguments.add("java");
        arguments.add("-jar");
        arguments.add("C:/src/sonarqube-9.9.2.77730/extensions/plugins/sonar-ballerina-plugin-1.0-all.jar");

        // if the user has passed the rule to be analyzed
        if (!userRule.equals("all")) {
            arguments.add("-Drule=" + userRule);
        }

        // Execute the sonar-scanner through a sub process
        ProcessBuilder processBuilder = new ProcessBuilder(arguments);
        try {
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outputStream.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//    @Override
//    public void scan(PrintStream outputStream) {
//        // Method 1: bal scan initiating sonar scan through process builder
//        // Setting up initial arguments to run the sonar-scanner depending on the OS
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
//        // By default, the sonar scan executed through ballerina will scan only ballerina files
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
//        // if the user has passed the rule to be analyzed
//        if (!userRule.equals("all")) {
//            arguments.add("-Drule=" + userRule);
//        }
//
//        // Execute the sonar-scanner through a sub process
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
//
//        // Method 2: bal scan initiating sonar scan through java class loaders
//        // executeThroughClassLoaders(outputStream);
//    }

    // Method 2: bal scan initiating sonar scan through java class loaders
    public void executeThroughClassLoaders(PrintStream outputStream) {
        // Create a class loader
        URLClassLoader cl = null;

        // To retrieve the absolute path of the sonar-scanner jar file in the bal scan tool
        ProtectionDomain protectionDomain = SonarQube.class.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();

        URL location = codeSource.getLocation();
        // Decoding the path to contain spaces instead of the %20 symbol
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(location.getPath(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        // The bal tool ScanCommand-all.jar file
        File currentJar = new File(decodedPath);

        // Creating absolute file path to the sonar-scanner-cli.jar from the bal tool jar
        File referencedJar = new File(currentJar.getParentFile()
                .getParentFile()
                .getParentFile(),
                "resources/sonar-scanner-cli-5.0.1.3006.jar");

        // Initiating the class loader
        try {
            // cl = new URLClassLoader(
            // new URL[]{
            // new File("C:/src/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-windows/lib/sonar-scanner-cli-5.0.1.3006.jar")
            cl = new URLClassLoader(new URL[]{referencedJar.toURI().toURL()}, ClassLoader.getSystemClassLoader());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // Retrieve the main class from the class loader
        Class mainClass = null;
        try {
            mainClass = cl.loadClass("org.sonarsource.scanner.cli.Main");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Retrieve the method to be called in the main class
        Method mainMethod = null;
        try {
            mainMethod = mainClass.getMethod("main", String[].class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // Call the method retrieved from the main class
        String[] args = new String[]{"-Dsonar.exclusions=" +
                "'" +
                "**/*.java," +
                "**/*.xml," +
                "**/*.yaml," +
                "**/*.go," +
                "**/*.kt," +
                "**/*.js," +
                "**/*.html," +
                "**/*.YAML," +
                "**/*.rb," +
                "**/*.scala," +
                "**/*.py" +
                "'"};

        try {
            // invoke(ifNonStaticThenObject, (Object) arguments)
            mainMethod.invoke(null, (Object) args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
