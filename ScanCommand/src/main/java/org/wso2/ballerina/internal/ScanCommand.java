package org.wso2.ballerina.internal;

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

    public final String PLATFORM_ARGS_PATTERN = "-PARG[\\w\\W]+=([\\w\\W]+)";
    private Map<String, String> platformArgs = new HashMap<>();
    private String projectPath = null;

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = "--target-dir", description = "target directory path")
    private Path targetDir;

    @CommandLine.Option(names = {"--scan-report"}, description = "enable scan report generation")
    private boolean scanReport;

    // TODO: To be removed once functionality added to Scan.toml
    @CommandLine.Option(names = {"--rules"},
            description = "Specify the rules to filter out specific issues from the analyzed results.",
            defaultValue = "all")
    private String userRule;

    @CommandLine.Option(names = {"--list-rules"},
            description = "List the internal rules available in the Ballerina scan tool.")
    private boolean listAllRules;

    @CommandLine.Option(names = {"--platforms"},
            description = "static code analysis output platform",
            defaultValue = "local")
    private String platforms;

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
        StringBuilder builder = helpMessage();
        out.append(builder);
    }

    @Override
    public void printUsage(StringBuilder out) {
        out.append("Tool for providing static code analysis results for Ballerina projects");
    }

    public StringBuilder helpMessage() {
        StringBuilder builder = new StringBuilder();
        builder.append("NAME\n");
        builder.append("\t" + "ballerina-scan - Static code analyzer" + "\n" + "\n\n");

        builder.append("SYNOPSIS\n");
        builder.append("\t" + "bal scan [OPTIONS] [<package>|<source-file>] [(-PARGkey=value)...]" + "\n " + "\n\n");

        builder.append("DESCRIPTION\n");
        builder.append("\t" + "Compiles and performs static code analysis and reports the analysis issues.\n" + "\t"
                + "Analyze source code defined in each module of a package when compiling the current package.\n" + "\t"
                + "It analyzes the given source file when compiling a single '.bal' file.\n" + "\n" + "\t"
                + "Note: Analyzing individual '.bal' files of a package is not allowed.\n" + "\n\n");

        builder.append("OPTIONS\n");
        builder.append("\t" + "--target-dir=<path>\n" + "\t" + "\t"
                + "Target directory path.\n\n");
        builder.append("\t" + "--scan-report\n" + "\t" + "\t"
                + "Generates an HTML report containing the analysis results.\n\n");
        builder.append("\t" + "--rules=<rule1, ...>\n" + "\t" + "\t"
                + "Specify internal rules to be enabled to extract specific issues from the analyzed results.\n\n");
        builder.append("\t" + "--list-rules\n" + "\t" + "\t"
                + "List the internal rules available in the Ballerina scan tool.\n\n");
        builder.append("\t" + "--platforms=<platformName1, ...>\n" + "\t" + "\t"
                + "Define platform plugin to report analysis results to. Before defining a platform,\n" + "\t" + "\t"
                + "the relevant plugin should be placed in the 'bre/libs' folder of the Ballerina distribution.\n" + "\t" + "\t"
                + "The user is able to define more than one platform.\n" + "\n\n");

        builder.append("ARGUMENTS\n");
        builder.append("\t" + "(-PARGkey=value)...\n" + "\t" + "\t"
                + "The list of platform arguments to be passed to the platform plugins when a scan is initiated\n" + "\n\n");

        builder.append("EXAMPLES\n");
        builder.append("\t" + "Run analysis against all Ballerina documents in the current package\n" + "\t" + "\t"
                + "$ bal scan\n\n");
        builder.append("\t" + "Run analysis against a standalone Ballerina file. The file path can be\n" + "\t"
                + "relative or absolute.\n" + "\t" + "\t"
                + "$ bal scan main.bal\n\n");
        builder.append("\t" + "Run analysis and save analysis results in specified directory.\n" + "\t" + "\t"
                + "$ bal scan --target-dir='results'\n\n");
        builder.append("\t" + "Run analysis and generate an analysis report.\n" + "\t" + "\t"
                + "$ bal scan --scan-report\n\n");
        builder.append("\t" + "Run analysis and filter out issues related to the specified rule.\n" + "\t" + "\t"
                + "$ bal scan --rules=S107\n\n");
        builder.append("\t" + "Run analysis and filter out issues related to multiple specified rules.\n" + "\t" + "\t"
                + "$ bal scan --rules='rule1, rule2, rule3'\n\n");
        builder.append("\t" + "Run analysis and report to sonarqube\n" + "\t" + "\t"
                + "$ bal scan --platforms=sonarqube\n\n");
        builder.append("\t" + "Run analysis and report to multiple platforms\n" + "\t" + "\t"
                + "$ bal scan --platforms='platformName1, platformName2, platformName3'\n\n");
        builder.append("\t" + "Run analysis and report to a platform with platform specific arguments\n" + "\t" + "\t"
                + "$ bal scan --platforms=platformName1 -PARGkey1=value1 -PARGkey2=val2\n");

        return builder;
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
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        if (!userRule.equals("all")) {
            boolean userDefinedRulesActivated = ScanUtils.activateUserDefinedRule(InbuiltRules.INBUILT_RULES, userRule);
            if (!userDefinedRulesActivated) {
                outputStream.println("Invalid rule: " + userRule);
                return;
            }
            ScanUtils.printRulesToConsole(InbuiltRules.INBUILT_RULES, outputStream);
        }

        if (listAllRules) {
            ScanUtils.printRulesToConsole(InbuiltRules.INBUILT_RULES, outputStream);
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

                outputStream.println("Running Scans");

                // Perform scan on ballerina file/project
                ArrayList<Issue> issues = localPlatform.analyzeProject(Path.of(userPath));

                // Stop reporting if there is no issues array
                if (issues == null) {
                    outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
                    return;
                }

                // TODO: Create logic filter out issues to only the rule IDs defined by the user in a Scan.toml file

                // Print results to console
                ScanUtils.printToConsole(issues, outputStream);

                outputStream.println();
                outputStream.println("Generating scan report...");

                // Save results to directory
                Path reportPath;
                if (targetDir != null) {
                    reportPath = ScanUtils.saveToDirectory(issues,
                            userPath,
                            targetDir.toString()
                    );
                } else {
                    reportPath = ScanUtils.saveToDirectory(issues,
                            userPath,
                            null);
                }

                outputStream.println();
                outputStream.println("View scan results at:");
                outputStream.println("\t" + reportPath + "\n");
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

    private void populatePlatformArguments(int subListStartValue) {
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

    private String checkPath() {
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