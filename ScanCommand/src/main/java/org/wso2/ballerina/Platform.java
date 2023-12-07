package org.wso2.ballerina;

import com.google.gson.JsonArray;
import org.wso2.ballerina.internal.platforms.Local;

import java.nio.file.Path;
import java.util.Set;

public abstract class Platform {
    // TODO: Make the initialize method to be triggered from bal scan tool using ServiceLoaders
    abstract public void initialize();

    // Function to send scanned results to analysis platforms
    public abstract void onScan(String scannedResults);
}