package org.wso2.ballerina;


import java.io.PrintStream;

import static org.wso2.ballerina.SonarQubeScanner.SOURCE_INVALID;

public class PackageValidator {
    public static void reportIssue(String userFile, PrintStream errorStream){
        // For now we will be displaying the rule violation output directly to the console
        String message = "Unable to parse file " + userFile;
        errorStream.println("Issue Type: " + SOURCE_INVALID);
        errorStream.println(message);
    }
}
