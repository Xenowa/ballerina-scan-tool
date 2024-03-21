/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.scan.utilities.ScanToolConstants;
import io.ballerina.scan.utilities.ScanUtils;
import io.ballerina.scan.utilities.StringToListConverter;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
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

@CommandLine.Command(name = "scan", description = "Perform static code analysis for ballerina packages")
public class ScanCommand implements BLauncherCmd {

    private final PrintStream outputStream;
    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();
    private String projectPath = null;
    private ScanTomlFile scanTomlFile;
    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"--quiet"}, hidden = true)
    private boolean quietFlag;

    @CommandLine.Option(names = "--target-dir", description = "Target directory path")
    private String targetDir;

    @CommandLine.Option(names = {"--scan-report"}, description = "Enable scan report generation")
    private boolean scanReport;

    @CommandLine.Option(names = {"--include-rules"},
            converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to include specific analysis issues")
    private List<String> includeRules = new ArrayList<>();

    @CommandLine.Option(names = {"--exclude-rules"},
            converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to exclude specific analysis issues")
    private List<String> excludeRules = new ArrayList<>();

    @CommandLine.Option(names = {"--list-rules"},
            description = "List the internal rules available in the Ballerina scan tool.")
    private boolean listRules;

    @CommandLine.Option(names = {"--platforms"},
            converter = StringToListConverter.class,
            description = "static code analysis output platform",
            defaultValue = "local")
    private List<String> platforms = new ArrayList<>();

    public ScanCommand() {
        this.outputStream = System.out;
    }

    public ScanCommand(PrintStream outputStream) {
        this.outputStream = outputStream;
    }

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
        InputStream inputStream = ScanCommand.class.getResourceAsStream("/ballerina-scan.help");
        StringBuilder builder = new StringBuilder();
        if (inputStream != null) {
            try (
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(inputStreamReader)
            ) {
                String content = br.readLine();
                builder.append(content);
                while ((content = br.readLine()) != null) {
                    builder.append("\n").append(content);
                }
            } catch (IOException e) {
                builder.append("Helper text is not available.");
                throw new RuntimeException(e);
            }
        }

        return builder;
    }

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {

    }

    // ========================
    // Scan Command Main Method
    // ========================
    @Override
    public void execute() {
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        // TODO: Service Load compiler plugins and retrieve their rules as well
        if (listRules) {
            ScanUtils.printRulesToConsole(InbuiltRules.INBUILT_RULES);
            return;
        }

        // TODO: Optimize project loading behaviour
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

        // Get all rules to include
        // From Scan.toml file
        Set<ScanTomlFile.RuleToFilter> rulesToInclude = scanTomlFile.getRulesToInclude();

        // Convert to String list
        List<String> allRulesToInclude = rulesToInclude.stream()
                .map(ScanTomlFile.RuleToFilter::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        // Add console defined rules
        allRulesToInclude.addAll(includeRules);

        // Get all rules to exclude
        // From Scan.toml file
        Set<ScanTomlFile.RuleToFilter> rulesToExclude = scanTomlFile.getRulesToExclude();

        // Convert to String list
        List<String> allRulesToExclude = rulesToExclude.stream()
                .map(ScanTomlFile.RuleToFilter::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        // Add console defined rules
        allRulesToExclude.addAll(excludeRules);

        // Perform scan on ballerina file/project
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(scanTomlFile, outputStream);
        ArrayList<Issue> issues = projectAnalyzer.analyzeProject(Path.of(userPath));

        // Remove rules not in include list
        if (!allRulesToInclude.isEmpty()) {
            issues.removeIf(issue -> !allRulesToInclude.contains(issue.getRuleID()));
        }

        // Remove rules in exclude list
        if (!allRulesToExclude.isEmpty()) {
            issues.removeIf(issue -> allRulesToExclude.contains(issue.getRuleID()));
        }

        // Stop reporting if there is no issues array
        if (issues == null) {
            outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
            return;
        }

        // TODO: Remove quiet flag with platformTriggered flag and move logic to platform plugin side
        // Produce analysis results locally if 'local' platform is given
        if (platforms.contains("local") && quietFlag) {
            // Save results to directory quietly
            if (targetDir != null) {
                ScanUtils.saveToDirectory(issues, userPath, targetDir);
            } else {
                ScanUtils.saveToDirectory(issues, userPath, null);
            }

            return; // Stop further execution after generating the report
        } else if (platforms.contains("local")) {
            // Print results to console
            ScanUtils.printToConsole(issues);
            if (scanReport) {
                Path scanReportPath;
                outputStream.println();
                outputStream.println("Generating scan report...");
                if (targetDir != null) {
                    scanReportPath = ScanUtils.generateScanReport(issues, userPath, targetDir);
                } else {
                    scanReportPath = ScanUtils.generateScanReport(issues, userPath, null);
                }
                outputStream.println();
                outputStream.println("View scan report at:");
                outputStream.println("\t" + ScanToolConstants.FILE_PROTOCOL + scanReportPath + "\n");
            }

            // Save results to directory
            Path reportPath;
            if (targetDir != null) {
                reportPath = ScanUtils.saveToDirectory(issues, userPath, targetDir);
            } else {
                reportPath = ScanUtils.saveToDirectory(issues, userPath, null);
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
            platforms.add(platform.getName());
            externalJarFilePaths.add(platform.getPath());

            // Map object map to string properties map
            platformArgs.put(platform.getName(),
                    platform.getArguments()
                            .entrySet()
                            .stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().toString())));
        });

        URLClassLoader ucl = loadExternalJars(externalJarFilePaths);

        ServiceLoader<StaticCodeAnalysisPlatformPlugin> scannerPlatformPlugins = ServiceLoader.load(
                StaticCodeAnalysisPlatformPlugin.class,
                ucl);

        // Proceed reporting to platforms if plugins exists
        if (scannerPlatformPlugins.stream().findAny().isPresent()) {
            scannerPlatformPlugins.forEach(staticCodeAnalysisPlatformPlugin -> {
                if (platforms.contains(staticCodeAnalysisPlatformPlugin.platform())) {
                    Map<String, String> platformSpecificArgs =
                            platformArgs.get(staticCodeAnalysisPlatformPlugin.platform());

                    staticCodeAnalysisPlatformPlugin.init(platformSpecificArgs);
                    staticCodeAnalysisPlatformPlugin.onScan(issues);

                    platforms.removeAll(Collections.singleton(staticCodeAnalysisPlatformPlugin.platform()));
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

    private String checkPath() {

        if (!argList.isEmpty()) {
            this.projectPath = String.valueOf(Paths.get(argList.get(0)));
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
                        Path parentPath = Path.of(userFilePath).toAbsolutePath().getParent();
                        return parentPath != null ? parentPath.toString() : "";
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
