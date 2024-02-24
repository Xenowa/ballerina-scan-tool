package io.ballerina.scan.utilities;

public class ScanToolConstants {

    // Internal issues constants
    public static final String CHECK_VIOLATION = "CHECK_VIOLATION";
    public static final String CODE_SMELL = "CODE_SMELL";
    public static final String BUG = "BUG";
    public static final String VULNERABILITY = "VULNERABILITY";
    public static final int SONARQUBE_RESERVED_RULES = 106;

    // External issues constants
    public static final String CUSTOM_CHECK_VIOLATION = "CUSTOM_CHECK_VIOLATION";
    public static final String MAIN_BAL = "main.bal";

    // Report generation constants
    public static final String RESULTS_HTML_FILE = "index.html";
    public static final String PATH_SEPARATOR = "/";
    public static final String TARGET_DIR_NAME = "target";
    public static final String RESULTS_JSON_FILE = "scan_results.json";
    public static final String FILE_PROTOCOL = "file://";
    public static final String REPORT_DATA_PLACEHOLDER = "__data__";

    // Scan.toml constants
    public static final String SCAN_FILE = "Scan.toml";
    public static final String SCAN_TABLE = "scan";
    public static final String SCAN_FILE_FIELD = "configPath";
    public static final String PLATFORM_TABLE = "platform";
    public static final String PLUGIN_TABLE = "plugin";
    public static final String RULES_TABLE = "rule";
    public static final String NEWLINE_SYMBOL = System.getProperty("line.separator");

    // Import generation constants
    public static final String USE_IMPORT_AS_SERVICE = " as _;";
    public static final String CUSTOM_RULES_COMPILER_PLUGIN_VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+$";
}
