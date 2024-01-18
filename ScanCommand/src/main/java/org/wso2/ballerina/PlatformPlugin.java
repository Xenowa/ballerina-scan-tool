package org.wso2.ballerina;

import java.util.ArrayList;
import java.util.Map;

public abstract class PlatformPlugin {
    // Function to retrieve platform extension name
    public abstract String platformName();

    // Function to initialize a platform plugin
    public abstract void initialize(Map<String, String> platformArgs);

    // Function to send analysis results to analysis platforms
    public abstract void onScan(ArrayList<Issue> issues);
}