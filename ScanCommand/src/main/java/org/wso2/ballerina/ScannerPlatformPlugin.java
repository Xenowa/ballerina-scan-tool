package org.wso2.ballerina;

import java.util.ArrayList;
import java.util.Map;

public interface ScannerPlatformPlugin {

    String platformName();

    void initialize(Map<String, String> platformArgs);

    void onScan(ArrayList<Issue> issues);
}
