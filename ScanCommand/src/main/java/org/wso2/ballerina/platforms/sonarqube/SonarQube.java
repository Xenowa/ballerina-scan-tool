package org.wso2.ballerina.platforms.sonarqube;

import com.google.gson.JsonObject;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.checks.functionChecks.FunctionChecks;
import org.wso2.ballerina.platforms.Platform;

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

public class SonarQube extends Platform {
    // Final attributes for determining the type of issues reported to Sonar Scanner
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String SOURCE_INVALID = "SOURCE_INVALID";

    public void scan(String userFile, PrintStream outputStream) {
        // executeThroughProcessBuilder(outputStream);
        executeThroughClassLoaders(outputStream);

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
//        // Output the JSON results to the console
//        outputStream.println(jsonOutput);
    }

    // Method 1: bal scan initiating sonar scan through process builder
    public void executeThroughProcessBuilder(PrintStream outputStream) {
        // Execute the sonar-scanner through a sub process
        ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "sonar-scanner", "-Dsonar.exclusions='**/*.java,**/*.xml,**/*.yaml,**/*.go,**/*.kt,**/*.js,**/*.html,**/*.YAML,**/*.rb,**/*.scala, **/*.py'");
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
        File referencedJar = new File(currentJar.getParentFile().getParentFile().getParentFile(), "resources/sonar-scanner-cli-5.0.1.3006.jar");

        // Initiating the class loader
        try {
            // cl = new URLClassLoader(new URL[]{new File("C:/src/sonar-scanner-cli/sonar-scanner-cli-5.0.1.3006-windows/lib/sonar-scanner-cli-5.0.1.3006.jar")
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
        String[] args = new String[]{"-Dsonar.exclusions='**/*.java,**/*.xml,**/*.yaml,**/*.go,**/*.kt,**/*.js,**/*.html,**/*.YAML,**/*.rb,**/*.scala, **/*.py'"};
        try {
            // invoke(ifNonStaticThenObject, (Object) arguments)
            mainMethod.invoke(null, (Object) args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
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
