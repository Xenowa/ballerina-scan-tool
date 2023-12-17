package org.wso2.ballerina;

import java.io.PrintStream;

public abstract class Platform {
    // TODO: Make the initialize method to be triggered from bal scan tool using ServiceLoaders
    // Following method should return back the name of the platform
    public abstract String initialize();

    // Function to send scanned results to analysis platforms
    public abstract void onScan(String analyzedReportPath, PrintStream outputStream, PrintStream errorStream);
}