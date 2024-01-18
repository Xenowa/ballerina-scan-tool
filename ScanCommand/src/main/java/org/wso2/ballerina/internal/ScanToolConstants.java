package org.wso2.ballerina.internal;

public class ScanToolConstants {
    // CommandLine constants
    public static final String PLATFORM_ARGS_PATTERN = "-PARG[\\w\\W]+=([\\w\\W]+)";

    // Internal issues constants
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";

    // External issues constants
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String CUSTOM_RULE_ID = "CUSTOM_RULE_ID";

    // Report generation constants
    public static final String PATH_SEPARATOR = "/";
    public static final String TARGET_DIR_NAME = "target";
    public static final String RESULTS_JSON_FILE = "scan_results.json";
    public static final String REPORT_DATA_PLACEHOLDER = "__data__";
}
