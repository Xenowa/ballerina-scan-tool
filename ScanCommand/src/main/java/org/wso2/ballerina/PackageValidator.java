package org.wso2.ballerina;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.PrintStream;

import static org.wso2.ballerina.SonarQubeScanner.SOURCE_INVALID;

public class PackageValidator {
    // Any issue which is not a rule violation issue would be logged through here
    public static void reportIssue(String userFile, PrintStream errorStream){
        JsonObject jsonObject = new JsonObject();

        // Create a JSON Object of the error
        jsonObject.addProperty("issueType", SOURCE_INVALID);
        String message = "Unable to parse file " + userFile;
        jsonObject.addProperty("message", message);

        // Convert the JSON output to print to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(jsonObject);

        // print the result to the console
        errorStream.println(jsonOutput);
    }
}
