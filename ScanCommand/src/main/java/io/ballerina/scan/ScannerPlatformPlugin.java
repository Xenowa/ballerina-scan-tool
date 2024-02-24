package io.ballerina.scan;

import java.util.ArrayList;
import java.util.Map;

public interface ScannerPlatformPlugin {

    String platformName();

    void initialize(Map<String, String> platformArgs);

    void onScan(ArrayList<Issue> issues);
}
