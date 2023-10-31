package org.wso2.ballerina;

import com.google.gson.JsonObject;

import static org.wso2.ballerina.SonarQubeScanner.SOURCE_INVALID;
import static org.wso2.ballerina.SonarQubeScanner.analysisIssues;

public class PackageValidator {
    // Any issue which is not a rule violation issue would be logged through here
    public void reportIssue(String userFile){
        JsonObject jsonObject = new JsonObject();

        // Create a JSON Object of the error
        jsonObject.addProperty("issueType", SOURCE_INVALID);
        String message = "Unable to parse file " + userFile;
        jsonObject.addProperty("message", message);

        // add the analysis issue to the issues array
        analysisIssues.add(jsonObject);
    }
}
