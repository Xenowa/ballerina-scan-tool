package org.wso2.ballerina.internal.platforms;

import org.apache.commons.lang3.SystemUtils;
import org.wso2.ballerina.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.wso2.ballerina.internal.ScanCommand.userRule;

// TODO:
//  - The following class file is to be removed and put into the sonar-ballerina plugin instead
public class SonarQube extends Platform {
    // TODO:
    //  - There should be an initialize method here
    //  - That method should trigger the sensor context from the sonar side
    //  - Next it should retrieve the absolute paths from the sonar sensor context
    //  - Next it should pass these absolute paths to the bal scan command
    //  - which can run using a process builder
    @Override
    public void initialize() {
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
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}