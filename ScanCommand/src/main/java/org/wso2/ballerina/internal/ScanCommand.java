package org.wso2.ballerina.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.cli.BLauncherCmd;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import io.ballerina.projects.Module;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.util.ProjectConstants;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.PlatformPlugin;
import org.wso2.ballerina.ToolAndCompilerPluginConnector;
import org.wso2.ballerina.internal.platforms.Local;
import picocli.CommandLine;

@CommandLine.Command(name = "scan", description = "Perform static code analysis for ballerina packages")
public class ScanCommand implements BLauncherCmd {
    // =============================
    // Ballerina Launcher Attributes
    // =============================
    private final PrintStream outputStream; // for success outputs
    private final PrintStream errorStream; // for error outputs

    public static final String PLATFORM_ARGS_PATTERN = "-PARG[\\w\\W]+=([\\w\\W]+)";
    private Map<String, String> platformArgs = new HashMap<>();
    private String projectPath = null;

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"--platforms"},
            description = "static code analysis output platform",
            defaultValue = "local")
    private String platforms;

    @CommandLine.Option(names = {"--rule"},
            description = "single rule to be checked during the static code analysis",
            defaultValue = "all")
    public static String userRule;

    public ScanCommand() {
        this.outputStream = System.out;
        this.errorStream = System.err;
    }

    public ScanCommand(PrintStream outputStream) {
        this.outputStream = outputStream;
        this.errorStream = outputStream;
    }


    // =====================
    // bal help INFO Methods
    // =====================
    @Override
    public String getName() {
        return "scan";
    }

    @Override
    public void printLongDesc(StringBuilder out) {
        out.append("Tool for providing static code analysis results for Ballerina projects\n\n");
        out.append("bal scan --platform=<option> <ballerina-file>\n\n");
        out.append("--option--\n");
        out.append("\toption 1: sonarqube\n");
        out.append("\toption 2: codeql\n");
        out.append("\toption 3: semgrep\n\n");
        out.append("i.e: bal scan --platform=sonarqube balFileName.bal\n");
    }

    @Override
    public void printUsage(StringBuilder out) {
        out.append("Tool for providing static code analysis results for Ballerina projects");
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    // ====================
    // Main Program Methods
    // ====================
    // MAIN method
    @Override
    public void execute() {
        // if bal scan --help is passed
        if (helpFlag) {
            StringBuilder builder = new StringBuilder();
            builder.append("Tool for providing static code analysis results for Ballerina projects\n\n");
            builder.append("bal scan --platform=<option> <ballerina-file>\n\n");
            builder.append("--option--\n");
            builder.append("\toption 1: sonarqube\n");
            builder.append("\toption 2: codeql\n");
            builder.append("\toption 3: semgrep\n\n");
            builder.append("i.e: bal scan --platform=sonarqube balFileName.bal\n");
            this.outputStream.println(builder);
            return;
        }

        // Trigger the relevant analysis platform
        switch (platforms) {
            case "local" -> {
                Local localPlatform = new Local();
                // proceed to retrieve the user provided filepath to perform a scan on if the platform was local
                String userPath;
                userPath = checkPath();
                if (userPath.equals("")) {
                    return;
                }

                // Perform scan on ballerina file/project
                ArrayList<Issue> issues = localPlatform.analyzeProject(Path.of(userPath));

                // Stop reporting if there is no issues array
                if (issues == null) {
                    outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
                    return;
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
                String jsonOutput = gson.toJson(issuesAsJson);
                outputStream.println(jsonOutput);
            }
            case "sonarqube" -> {
                Local localPlatform = new Local();
                // proceed to retrieve the user provided filepath to perform a scan on if the platform was local
                String userPath;
                userPath = checkPath();
                if (userPath.equals("")) {
                    return;
                }

                // Perform scan on ballerina file/project
                ArrayList<Issue> issues = localPlatform.analyzeProject(Path.of(userPath));

                // Stop reporting if there is no analysis results
                if (issues == null) {
                    outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
                    return;
                }

                // Report results to relevant platform plugins
                ArrayList<String> externalJarFilePaths = new ArrayList<>();
                externalJarFilePaths.add("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\sonarqube-platform-plugin\\build\\libs\\sonarqube-platform-plugin-1.0.jar");

                URLClassLoader ucl = loadExternalJars(externalJarFilePaths);

                // Platform plugins for reporting
                ServiceLoader<PlatformPlugin> platformPlugins = ServiceLoader.load(PlatformPlugin.class, ucl);
                // Iterate through the loaded interfaces
                for (PlatformPlugin platformPlugin : platformPlugins) {
                    // Retrieve the platform name through initialize method
                    String platformName = platformPlugin.initialize(platformArgs);

                    // If a valid platform name is provided then trigger reporting
                    // For now we will make the initialize return null in other platforms
                    if (platformName != null) {
                        platformPlugin.onScan(issues);
                    }
                }
            }
            case "codeql", "semgrep" -> {
                outputStream.println("Platform support is not available yet!");
            }
            case "debug" -> {
                // Simulate loading a project and engaging a compiler plugin
                String userPath;
                userPath = checkPath();
                // Array to hold all issues
                ArrayList<Issue> issues = new ArrayList<>();

                // Get access to the project API
                Project project = ProjectLoader.loadProject(Path.of(userPath));

                // Iterate through each module of the project
                project.currentPackage().moduleIds().forEach(moduleId -> {
                    // Get access to the project modules
                    Module module = project.currentPackage().module(moduleId);

                    // ========
                    // METHOD 2 (using compiler plugins with URLClassLoaders and service loaders)
                    // ========
                    // This method aims to pass Issues without using Ballerina compiler diagnostics
                    // Load the compiler plugin
                    URL jarUrl;

                    try {
                        jarUrl = new File("C:\\Users\\Tharana Wanigaratne\\.ballerina\\repositories\\central.ballerina.io\\bala\\tharana_wanigaratne\\compiler_plugin_issueContextShareTesting\\0.1.0\\java17\\compiler-plugin\\libs\\issue-context-share-test-plugin-1.0-all.jar")
                                .toURI()
                                .toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }

                    URLClassLoader externalJarClassLoader = new URLClassLoader(new URL[]{jarUrl},
                            this.getClass().getClassLoader());

                    ServiceLoader<ToolAndCompilerPluginConnector> externalScannerJars = ServiceLoader.load(
                            ToolAndCompilerPluginConnector.class,
                            externalJarClassLoader);

                    // Iterate through the loaded interfaces
                    String messageFromTool = "Sent from Ballerina Scan Tool";
                    for (ToolAndCompilerPluginConnector externalScannerJar : externalScannerJars) {
                        // Call the interface method and pass a context
                        externalScannerJar.sendMessageFromTool(messageFromTool);
                    }

                    if (module.isDefaultModule()) {
                        // Compile the project and engage the plugin once
                        // If context has been passed correctly it will be displayed in the console
                        project.currentPackage().getCompilation();
                    }
                });
            }
            default -> outputStream.println("Platform provided is invalid, run bal scan --help for more info!");
        }
    }

    public void populatePlatformArguments(int subListStartValue) {
        String[] argumentsArray = argList.subList(subListStartValue, argList.size()).toArray(new String[0]);
        // Iterate through the arguments and populate the HashMap
        for (String argument : argumentsArray) {
            // Check if the argument matches the -PARG pattern
            if (argument.matches(PLATFORM_ARGS_PATTERN)) {
                // Split each argument into key and value based on "="
                String[] keyValue = argument.split("=");

                // Check if the argument is in the correct format (contains "=")
                if (keyValue.length == 2) {
                    // Add the key-value pair to the HashMap
                    this.platformArgs.put(keyValue[0].split("-PARG")[1], keyValue[1]);
                } else {
                    // Handle invalid arguments (optional)
                    System.out.println("Invalid argument: " + argument);
                }
            }
        }
    }

    public String checkPath() {
        if (!argList.isEmpty()) {
            if (!argList.get(0).matches(PLATFORM_ARGS_PATTERN)) {
                // Check if the first argument is not a platform argument and retrieve the file path
                this.projectPath = String.valueOf(Paths.get(argList.get(0)));
                if (argList.size() > 1) {
                    populatePlatformArguments(1);
                }
            } else {
                populatePlatformArguments(0);
            }
        }

        // retrieve the user passed argument or the current working directory
        String userFilePath = this.projectPath != null ? this.projectPath : System.getProperty("user.dir");

        // Check if the user provided path is a file or a directory
        File file = new File(userFilePath);
        if (file.exists()) {
            if (file.isFile()) {
                // Check if the file extension is '.bal'
                if (!userFilePath.endsWith(ProjectConstants.BLANG_SOURCE_EXT)) {
                    this.outputStream.println("Invalid file format received!\n File format should be of type '.bal'");
                    return "";
                } else {
                    // Perform check if the user has provided the file in "./balFileName.bal" format and if so remove
                    // the trailing slash
                    if (userFilePath.startsWith("./") || userFilePath.startsWith(".\\")) {
                        userFilePath = userFilePath.substring(2);
                    }

                    return userFilePath;
                }
            } else {
                // If it's a directory, validate it's a ballerina build project
                File ballerinaTomlFile = new File(userFilePath, ProjectConstants.BALLERINA_TOML);
                if (!ballerinaTomlFile.exists() || !ballerinaTomlFile.isFile()) {
                    this.outputStream.println("ballerina: Invalid Ballerina package directory: " +
                            userFilePath +
                            ", cannot find 'Ballerina.toml' file.");
                    return "";
                } else {
                    // Following is to mitigate the issue when "." is encountered in the scanning process
                    if (userFilePath.equals(".")) {
                        return Path.of(userFilePath)
                                .toAbsolutePath()
                                .getParent()
                                .toString();
                    }

                    return userFilePath;
                }
            }
        } else {
            this.outputStream.println("No such file or directory exists!\n Please check the file path and" +
                    "then re-run the command.");
            return "";
        }
    }

    private URLClassLoader loadExternalJars(ArrayList<String> jarPaths) {
        ArrayList<URL> jarUrls = new ArrayList<>();

        jarPaths.forEach(jarPath -> {
            try {
                URL jarUrl = Path.of(jarPath).toUri().toURL();
                jarUrls.add(jarUrl);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });

        return new URLClassLoader(jarUrls.toArray(new URL[0]), this.getClass().getClassLoader());
    }
}