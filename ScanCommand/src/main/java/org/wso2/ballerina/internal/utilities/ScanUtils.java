package org.wso2.ballerina.internal.utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.projects.BallerinaToml;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.TomlDocument;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.internal.model.Target;
import io.ballerina.toml.api.Toml;
import io.ballerina.toml.semantic.ast.TomlValueNode;
import org.wso2.ballerina.Issue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.ballerina.projects.util.ProjectConstants.CENTRAL_REPOSITORY_CACHE_NAME;
import static io.ballerina.projects.util.ProjectConstants.LOCAL_REPOSITORY_NAME;
import static io.ballerina.projects.util.ProjectConstants.REPORT_DIR_NAME;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.NEWLINE_SYMBOL;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.PLATFORM_TABLE;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.PLUGIN_TABLE;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.RESULTS_JSON_FILE;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.RULES_TABLE;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.SCAN_FILE;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.SCAN_FILE_FIELD;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.SCAN_TABLE;
import static org.wso2.ballerina.internal.utilities.ScanToolConstants.TARGET_DIR_NAME;

public class ScanUtils {
    private ScanUtils() {
    }

    public static void printToConsole(ArrayList<Issue> issues, PrintStream outputStream) {
        String jsonOutput = convertIssuesToJsonString(issues);

        outputStream.println();
        outputStream.println(jsonOutput);
    }

    public static String convertIssuesToJsonString(ArrayList<Issue> issues) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        return gson.toJson(issuesAsJson);
    }

    public static Target getTargetPath(Project project, String directoryName) {
        Target target;
        try {
            if (directoryName != null) {
                String file = project.sourceRoot().toString() + ScanToolConstants.PATH_SEPARATOR + directoryName;
                Path tempDirectory = Files.createDirectory(Path.of(file));
                target = new Target(tempDirectory);
            } else if (project.kind() == ProjectKind.BUILD_PROJECT) {
                target = new Target(project.targetDir());
            } else {
                Path tempDirectory = Files.createTempDirectory(TARGET_DIR_NAME
                        + System.nanoTime());
                target = new Target(tempDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return target;
    }

    public static Path saveToDirectory(ArrayList<Issue> issues, String projectPath, String directoryName) {
        // Create folder to save issues to
        Project project = ProjectLoader.loadProject(Path.of(projectPath));
        Target target = getTargetPath(project, directoryName);

        // Retrieve path where report is saved
        Path jsonFilePath;
        try {
            Path reportPath = target.getReportPath();

            // Convert the issues to a json string
            String jsonOutput = convertIssuesToJsonString(issues);

            // Create the file to save the analysis issues to
            File jsonFile = new File(reportPath.resolve(RESULTS_JSON_FILE).toString());

            // Write results to file and return saved file path
            try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFile)) {
                try (Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                    writer.write(new String(jsonOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                    writer.close();

                    jsonFilePath = jsonFile.toPath();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jsonFilePath;
    }

    // Save scan results in the HTML template
    public static Path generateScanReport(ArrayList<Issue> issues, String projectPath, String directoryName) {
        // Convert existing issues to the structure required by the scan report
        Project project = ProjectLoader.loadProject(Path.of(projectPath));
        JsonObject jsonScannedProject = new JsonObject();
        jsonScannedProject.addProperty("projectName", project.currentPackage().packageName().toString());

        Map<String, JsonObject> jsonScanReportPathAndFile = new HashMap<>();
        issues.forEach((issue) -> {
            String filePath = issue.getReportedFilePath();
            if (!jsonScanReportPathAndFile.containsKey(filePath)) {
                JsonObject jsonScanReportFile = new JsonObject();

                jsonScanReportFile.addProperty("fileName", issue.getFileName());
                jsonScanReportFile.addProperty("filePath", filePath);

                // Get the contents of the file through a file reader
                String fileContent = "";
                try {
                    fileContent = Files.readString(Path.of(filePath));
                } catch (Exception ignored) {
                }
                jsonScanReportFile.addProperty("fileContent", fileContent);

                JsonObject jsonScanReportIssueTextRange = new JsonObject();
                jsonScanReportIssueTextRange.addProperty("startLine", issue.getStartLine());
                jsonScanReportIssueTextRange.addProperty("startLineOffset", issue.getStartLineOffset());
                jsonScanReportIssueTextRange.addProperty("endLine", issue.getEndLine());
                jsonScanReportIssueTextRange.addProperty("endLineOffset", issue.getEndLineOffset());

                JsonObject jsonScanReportIssue = new JsonObject();
                jsonScanReportIssue.addProperty("ruleID", issue.getRuleID());
                jsonScanReportIssue.addProperty("type", issue.getType());
                jsonScanReportIssue.addProperty("issueType", issue.getIssueType());
                jsonScanReportIssue.addProperty("message", issue.getMessage());
                jsonScanReportIssue.add("textRange", jsonScanReportIssueTextRange);

                JsonArray jsonIssues = new JsonArray();
                jsonIssues.add(jsonScanReportIssue);
                jsonScanReportFile.add("issues", jsonIssues);

                jsonScanReportPathAndFile.put(filePath, jsonScanReportFile);
            } else {
                JsonObject jsonScanReportFile = jsonScanReportPathAndFile.get(filePath);

                JsonObject jsonScanReportIssueTextRange = new JsonObject();
                jsonScanReportIssueTextRange.addProperty("startLine", issue.getStartLine());
                jsonScanReportIssueTextRange.addProperty("startLineOffset", issue.getStartLineOffset());
                jsonScanReportIssueTextRange.addProperty("endLine", issue.getEndLine());
                jsonScanReportIssueTextRange.addProperty("endLineOffset", issue.getEndLineOffset());

                JsonObject jsonScanReportIssue = new JsonObject();
                jsonScanReportIssue.addProperty("ruleID", issue.getRuleID());
                jsonScanReportIssue.addProperty("type", issue.getType());
                jsonScanReportIssue.addProperty("issueType", issue.getIssueType());
                jsonScanReportIssue.addProperty("message", issue.getMessage());
                jsonScanReportIssue.add("textRange", jsonScanReportIssueTextRange);

                JsonArray jsonIssues = jsonScanReportFile.getAsJsonArray("issues");
                jsonIssues.add(jsonScanReportIssue);

                jsonScanReportFile.add("issues", jsonIssues);
                jsonScanReportPathAndFile.put(filePath, jsonScanReportFile);
            }
        });

        JsonArray jsonScannedFiles = new JsonArray();
        jsonScanReportPathAndFile.values().forEach(jsonScannedFiles::add);
        jsonScannedProject.add("scannedFiles", jsonScannedFiles);

        // Get the JSON Output of the issues
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(jsonScannedProject);

        // Dump to the scan html report
        // Get the current target directory
        Target target = getTargetPath(project, directoryName);

        // Access the inner JAR zip
        InputStream innerJarStream = ScanUtils.class.getResourceAsStream("/report.zip");
        String content;
        File htmlFile = null;
        try {
            unzipReportResources(innerJarStream, target.getReportPath().toFile());

            // Read all content in the html file
            content = Files.readString(target.getReportPath().resolve(ScanToolConstants.RESULTS_HTML_FILE));

            // Replace __data__ placeholder in the html file
            content = content.replace(ScanToolConstants.REPORT_DATA_PLACEHOLDER, jsonOutput);

            // Overwrite the html file
            htmlFile = new File(target.getReportPath().resolve(ScanToolConstants.RESULTS_HTML_FILE).toString());
            FileOutputStream fileOutputStream = new FileOutputStream(htmlFile);
            try (Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                writer.write(new String(content.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // Return the file path
        return htmlFile.toPath();
    }

    public static void unzipReportResources(InputStream source, File target) throws IOException {
        final ZipInputStream zipStream = new ZipInputStream(source);
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            if (!nextEntry.isDirectory()) {
                final File nextFile = new File(target, nextEntry.getName());

                // create directories
                final File parent = nextFile.getParentFile();
                if (parent != null) {
                    Files.createDirectories(parent.toPath());
                }

                // write file
                try (OutputStream targetStream = new FileOutputStream(nextFile)) {
                    final int bufferSize = 4 * 1024;
                    final byte[] buffer = new byte[bufferSize];
                    int nextCount;
                    while ((nextCount = zipStream.read(buffer)) >= 0) {
                        targetStream.write(buffer, 0, nextCount);
                    }
                }
            }
        }
    }

    public static void printRulesToConsole(HashMap<String, Rule> rules, PrintStream outputStream) {
        outputStream.println("Default available rules:");

        outputStream.println("\t" + "RuleID" + "\t"
                + " | " + "Rule Activated" + "\t"
                + " | " + "Rule Description" + "\n" + "\t"
                + "---------------------------------------------------");

        rules.forEach((ruleID, rule) -> {
            outputStream.println("\t" + ruleID + "\t"
                    + " | " + rule.ruleIsActivated()
                    + (rule.ruleIsActivated() ? "\t" + "\t" + "\t" : "\t" + "\t")
                    + " | " + rule.getRuleDescription());
        });

        outputStream.println();
    }

    public static boolean activateUserDefinedRule(Map<String, Rule> rules, List<String> userDefinedRules) {
        AtomicBoolean userDefinedRulesActivated = new AtomicBoolean(true);

        // Disable all inbuilt rules
        rules.values().forEach(inbuiltRule -> {
            inbuiltRule.setRuleIsActivated(false);
        });

        userDefinedRules.forEach(userDefinedRule -> {
            // If even a single user defined rule does not exist in the inbuilt rules return false
            if (!rules.containsKey(userDefinedRule)) {
                userDefinedRulesActivated.set(false);
            } else {
                rules.get(userDefinedRule).setRuleIsActivated(true);
            }
        });

        return userDefinedRulesActivated.get();
    }

    public static ScanTomlFile retrieveScanTomlConfigurations(String projectPath) {
        Path ballerinaProjectPath = Path.of(projectPath);
        Project project = ProjectLoader.loadProject(ballerinaProjectPath);

        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            // Retrieve the Ballerina.toml from the Ballerina project
            if (project.currentPackage().ballerinaToml().isPresent()) {
                BallerinaToml ballerinaToml = project.currentPackage().ballerinaToml().get();

                // Retrieve it as a document
                TomlDocument ballerinaTomlDocument = ballerinaToml.tomlDocument();

                // Parse the toml document
                Toml ballerinaTomlDocumentContent = ballerinaTomlDocument.toml();

                // Retrieve only the [Scan] Table values
                Toml scanTable = ballerinaTomlDocumentContent.getTable(SCAN_TABLE).orElse(null);

                // Load Scan.toml configurations
                if (scanTable != null) {
                    // Retrieve the Scan.toml file path
                    TomlValueNode configPath = scanTable.get(SCAN_FILE_FIELD).orElse(null);

                    if (configPath != null) {
                        String scanTomlPath = configPath.toNativeValue().toString();
                        if (new File(scanTomlPath).exists()) {
                            Path scanTomlFilePath = Path.of(configPath.toNativeValue().toString());
                            return loadScanFile(scanTomlFilePath);
                        } else {
                            try {
                                URL url = new URL(scanTomlPath);
                                return loadRemoteScanFile(ballerinaProjectPath, url);
                            } catch (Exception ignored) {
                                System.out.println("File: " + scanTomlPath + " does not exists!");
                                return new ScanTomlFile();
                            }
                        }
                    }

                    System.out.println("configPath for Scan.toml is missing!");
                    return new ScanTomlFile();
                }

                // Try to find a 'Scan.toml' file in the default project
                Path scanTomlFilePath = Path.of(SCAN_FILE);
                if (Files.exists(scanTomlFilePath)) {
                    System.out.println("Loading scan tool configurations from " + scanTomlFilePath.toString() + "...");
                    return loadScanFile(scanTomlFilePath);
                }

                // If there is no local 'Scan.toml' file then load an empty in memory scan toml configuration
                return new ScanTomlFile();
            }
            return new ScanTomlFile();
        }
        return new ScanTomlFile();
    }

    public static ScanTomlFile loadRemoteScanFile(Path root, URL remoteScanTomlFilePath) {
        //  2. If 'Scan.toml' is already available in cache load it from there
        Path cachePath = root.resolve(TARGET_DIR_NAME).resolve(REPORT_DIR_NAME).resolve(SCAN_FILE);
        if (Files.exists(cachePath)) {
            System.out.println("Loading scan tool configurations from cache...");
            return loadScanFile(cachePath);
        }

        //  3. Else download/copy the configurations from the remote Scan.toml and create a 'Scan.toml' from it and
        // send configurations from it
        StringBuilder fileContent = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection) remoteScanTomlFilePath.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect();
                System.out.println("Unable to load scan tool configurations from: "
                        + remoteScanTomlFilePath.toString());
                return new ScanTomlFile();
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append(NEWLINE_SYMBOL);
                }
            }
            connection.disconnect();
        } catch (IOException e) {
            System.out.println("Unable to load scan tool configurations from: "
                    + remoteScanTomlFilePath.toString());
            return new ScanTomlFile();
        }

        // Cache remote file
        cacheScanFile(root, fileContent.toString());
        // Load file from cache
        System.out.println("Loading scan tool configurations from " + remoteScanTomlFilePath.toString());
        return loadScanFile(cachePath);
    }

    public static void cacheScanFile(Path root, String fileContent) {
        Path targetDir = root.resolve(TARGET_DIR_NAME);
        if (!Files.exists(targetDir)) {
            try {
                Files.createDirectory(targetDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Path reportDir = targetDir.resolve(REPORT_DIR_NAME);
        if (!Files.exists(reportDir)) {
            try {
                Files.createDirectory(reportDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        String filePath = reportDir.resolve(SCAN_FILE).toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            writer.write(fileContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ScanTomlFile loadScanFile(Path scanTomlFilePath) {
        // Parse the toml document
        Toml scanTomlDocumentContent;
        try {
            scanTomlDocumentContent = Toml.read(scanTomlFilePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Start creating the Scan.toml object
        ScanTomlFile scanTomlFile = new ScanTomlFile();

        // Retrieve all platform tables
        List<Toml> platformTables = scanTomlDocumentContent.getTables(PLATFORM_TABLE);
        platformTables.forEach(platformTable -> {
            Map<String, Object> properties = platformTable.toMap();
            String name = !(properties.get("name") instanceof String) ? null :
                    properties.remove("name").toString();
            String path = !(properties.get("path") instanceof String) ? null :
                    properties.remove("path").toString();

            if (name != null && !name.isEmpty() && path != null && Files.exists(Path.of(path))) {
                ScanTomlFile.Platform platform = new ScanTomlFile.Platform(name, path, properties);
                scanTomlFile.setPlatform(platform);
            }
        });

        // Retrieve all custom rule compiler plugin tables
        List<Toml> compilerPluginTables = scanTomlDocumentContent.getTables(PLUGIN_TABLE);
        compilerPluginTables.forEach(compilerPluginTable -> {
            Map<String, Object> properties = compilerPluginTable.toMap();
            String org = !(properties.get("org") instanceof String) ? null :
                    properties.get("org").toString();
            String name = !(properties.get("name") instanceof String) ? null :
                    properties.get("name").toString();
            String version = !(properties.get("version") instanceof String) ? null :
                    properties.get("version").toString();
            String repository = !(properties.get("repository") instanceof String) ? null :
                    properties.get("repository").toString();

            if (org != null && !org.isEmpty() &&
                    name != null && !name.isEmpty() &&
                    version != null && !version.isEmpty()) {
                ScanTomlFile.Plugin plugin;
                if (repository != null) {
                    plugin = new ScanTomlFile.Plugin(org,
                            name,
                            version,
                            repository.equals(LOCAL_REPOSITORY_NAME) ||
                                    repository.equals(CENTRAL_REPOSITORY_CACHE_NAME) ?
                                    repository : null);
                } else {
                    plugin = new ScanTomlFile.Plugin(org,
                            name,
                            version,
                            null);
                }
                scanTomlFile.setPlugin(plugin);
            }
        });

        // Retrieve all filter rule tables
        // [rules]
        // ids = "S107, S108"
        Toml rulesTable = scanTomlDocumentContent.getTable(RULES_TABLE).orElse(null);
        if (rulesTable != null) {
            TomlValueNode ids = rulesTable.get("ids").orElse(null);
            if (ids != null) {
                String stringIds = ids.toNativeValue().toString();
                List<String> rules = Arrays.asList(stringIds.split("\\s*,\\s*"));
                rules.forEach(rule -> {
                    ScanTomlFile.RuleToFilter ruleToFilter = new ScanTomlFile.RuleToFilter(rule);
                    scanTomlFile.setRuleToFilter(ruleToFilter);
                });
            }
        }

        return scanTomlFile;
    }
}
