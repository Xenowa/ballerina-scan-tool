package org.wso2.ballerina;

import java.util.ArrayList;
import java.util.Map;

public abstract class PlatformPlugin {
    // Method to initialize a platform plugin and return name of the platform
    public abstract String initialize(Map<String, String> platformArgs);

    // Function to send scanned results to analysis platforms
    public abstract void onScan(ArrayList<Issue> issues);
}