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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

import io.ballerina.projects.util.ProjectConstants;
import org.wso2.ballerina.Issue;
import org.wso2.ballerina.PlatformPlugin;
import org.wso2.ballerina.internal.utilities.ScanTomlFile;
import org.wso2.ballerina.internal.utilities.ScanToolConstants;
import org.wso2.ballerina.internal.utilities.ScanUtils;
import org.wso2.ballerina.internal.utilities.StringToListConverter;
import picocli.CommandLine;

@CommandLine.Command(name = "scan", description = "Perform static code analysis for ballerina packages")
public class ScanCommand implements BLauncherCmd {
    // =============================
    // Ballerina Launcher Attributes
    // =============================
    private final PrintStream outputStream; // for success outputs
    private final PrintStream errorStream; // for error outputs
    private Map<String, String> commandlinePlatformArgs = new HashMap<>();
    private String projectPath = null;
    private ScanTomlFile scanTomlFile;

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = "--target-dir", description = "Target directory path")
    private Path targetDir;

    @CommandLine.Option(names = {"--scan-report"}, description = "Enable scan report generation")
    private boolean scanReport;

    // TODO: To be removed once functionality added to Scan.toml
    @CommandLine.Option(names = {"--rules"},
            converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to filter specific analysis issues")
    private List<String> userRules = new ArrayList<>();

    @CommandLine.Option(names = {"--list-rules"},
            description = "List the internal rules available in the Ballerina scan tool.")
    private boolean listAllRules;

    @CommandLine.Option(names = {"--platforms"},
            converter = StringToListConverter.class,
            description = "static code analysis output platform",
            defaultValue = "local")
    private List<String> platforms = new ArrayList<>();

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
                + "the relevant plugin path should be provided in a ‘Scan.toml’ file.\n" + "\t" + "\t"
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

        if (!userRules.isEmpty()) {
            boolean userDefinedRulesActivated = ScanUtils.activateUserDefinedRule(InbuiltRules.INBUILT_RULES,
                    userRules);
            if (!userDefinedRulesActivated) {
                outputStream.println("Invalid rules list: " + userRules);
                return;
            }
            ScanUtils.printRulesToConsole(InbuiltRules.INBUILT_RULES, outputStream);
        }

        if (listAllRules) {
            ScanUtils.printRulesToConsole(InbuiltRules.INBUILT_RULES, outputStream);
            return;
        }

        // Retrieve project location
        String userPath;
        userPath = checkPath();
        if (userPath.equals("")) {
            return;
        }

        outputStream.println();
        outputStream.println("Running Scans");

        // Retrieve scan.toml file configurations
        scanTomlFile = ScanUtils.retrieveScanTomlConfigurations(userPath);

        // Perform scan on ballerina file/project
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer();
        ArrayList<Issue> issues = projectAnalyzer.analyzeProject(Path.of(userPath), scanTomlFile);

        // Stop reporting if there is no issues array
        if (issues == null) {
            outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
            return;
        }

        // filter user defined rules if present in Scan.toml file
        Set<ScanTomlFile.RuleToFilter> rulesToFilter = scanTomlFile.getRulesToFilter();
        if (!rulesToFilter.isEmpty()) {
            issues.removeIf(issue -> rulesToFilter.stream()
                    .noneMatch(ruleToFilter -> ruleToFilter.getId()
                            .equals(issue.getRuleID())));
        }

        // Produce analysis results locally if 'local' platform is given
        if (platforms.contains("local")) {
            // Print results to console
            ScanUtils.printToConsole(issues, outputStream);

            if (scanReport) {
                Path scanReportPath;
                outputStream.println();
                outputStream.println("Generating scan report...");
                if (targetDir != null) {
                    scanReportPath = ScanUtils.generateScanReport(issues,
                            userPath,
                            targetDir.toString());
                } else {
                    scanReportPath = ScanUtils.generateScanReport(issues,
                            userPath,
                            null);
                }
                outputStream.println();
                outputStream.println("View scan report at:");
                outputStream.println("\t" + ScanToolConstants.FILE_PROTOCOL + scanReportPath + "\n");
            }

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

            platforms.removeAll(Collections.singleton("local"));
        }

        // Retrieve the platform JAR file paths and arguments from Scan.toml
        ArrayList<String> externalJarFilePaths = new ArrayList<>();
        Map<String, Map<String, String>> platformArgs = new HashMap<>();
        scanTomlFile.getPlatforms().forEach(platform -> {
            if (platforms.contains(platform.getName())) {
                externalJarFilePaths.add(platform.getPath());

                // Map object map to string properties map
                platformArgs.put(platform.getName(),
                        platform.getArguments()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().toString())));
            }
        });

        URLClassLoader ucl = loadExternalJars(externalJarFilePaths);

        ServiceLoader<PlatformPlugin> platformPlugins = ServiceLoader.load(PlatformPlugin.class, ucl);

        // Proceed reporting to platforms if plugins exists
        if (platformPlugins.stream().findAny().isPresent()) {
            platformPlugins.forEach(platformPlugin -> {
                if (platforms.contains(platformPlugin.platformName())) {
                    Map<String, String> platformSpecificArgs = platformArgs.get(platformPlugin.platformName());
                    if (!commandlinePlatformArgs.isEmpty()) {
                        platformSpecificArgs.putAll(commandlinePlatformArgs);
                    }

                    platformPlugin.initialize(platformSpecificArgs);
                    platformPlugin.onScan(issues);

                    platforms.removeAll(Collections.singleton(platformPlugin.platformName()));
                }
            });
        }

        // If there are any platforms remaining which were not found in platform plugin JARs
        platforms.forEach(remainingPlatform -> {
            switch (remainingPlatform) {
                case "semgrep", "codeql" -> {
                    outputStream.println();
                    outputStream.println(remainingPlatform + " platform support is not available yet!");
                }
                default -> {
                    outputStream.println();
                    outputStream.println("Error: The specified platform '" + remainingPlatform + "' is not available.");
                    outputStream.println("Please ensure that the required platform plugin is installed and its path" +
                            " is correctly specified in Scan.toml.");
                }
            }
        });
    }

    private void populatePlatformArguments(int subListStartValue) {
        String[] argumentsArray = argList.subList(subListStartValue, argList.size()).toArray(new String[0]);
        // Iterate through the arguments and populate the HashMap
        for (String argument : argumentsArray) {
            // Check if the argument matches the -PARG pattern
            if (argument.matches(ScanToolConstants.PLATFORM_ARGS_PATTERN)) {
                // Split each argument into key and value based on "="
                String[] keyValue = argument.split("=");

                // Check if the argument is in the correct format (contains "=")
                if (keyValue.length == 2) {
                    // Add the key-value pair to the HashMap
                    this.commandlinePlatformArgs.put(keyValue[0].split("-PARG")[1], keyValue[1]);
                } else {
                    // Handle invalid arguments (optional)
                    System.out.println("Invalid argument: " + argument);
                }
            }
        }
    }

    private String checkPath() {
        if (!argList.isEmpty()) {
            if (!argList.get(0).matches(ScanToolConstants.PLATFORM_ARGS_PATTERN)) {
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