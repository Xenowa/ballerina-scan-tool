package org.wso2.ballerina;

import com.google.gson.JsonArray;
import org.wso2.ballerina.internal.platforms.Local;

import java.nio.file.Path;
import java.util.Set;

public abstract class Platform {
    /*
     * method for external platforms to extend the bal scan tool
     * call the onScan method within this to trigger the static code analysis of the files in each project
     * */
    abstract public void initialize();

    /*
     * method to trigger the scanning from the external platform when the absolute paths are sent to it
     * */
    // TODO:
    //  - Since the sonar-ballerina plugin only requires the analyzed file results without the project the files
    //  were on, we can straight up include all files only without the project itself
    public JsonArray onScan(Set<Path> projectFolderPaths) {
        // Initiate an instance of the local platform
        Local localPlatform = new Local();

        // Array to hold analysis results of ballerina files
        JsonArray allAnalyzedFiles = new JsonArray();

        // bal scan projectFolderPath
        projectFolderPaths.forEach(projectFolderPath -> {
            // Retrieve all the analyzedFiles per project
            JsonArray analyzedFiles = localPlatform.analyzeProject(projectFolderPath);

            // Put analyzed files of a project into all analyzed files
            allAnalyzedFiles.addAll(analyzedFiles);
        });

        // Return all analyzed files
        return allAnalyzedFiles;
    }
}