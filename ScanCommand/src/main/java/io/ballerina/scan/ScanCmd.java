/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan;

import io.ballerina.cli.BLauncherCmd;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.directory.SingleFileProject;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.scan.utilities.ScanUtils;
import io.ballerina.scan.utilities.StringToListConverter;
import picocli.CommandLine;

import java.io.BufferedReader;
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

import static io.ballerina.scan.ScanToolConstants.SCAN_COMMAND;

@CommandLine.Command(name = SCAN_COMMAND, description = "Perform static code analysis for ballerina packages")
public class ScanCmd implements BLauncherCmd {

    private final PrintStream outputStream;

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = "--platform-triggered",
            description = "Specify whether the scan command is triggered from an external analysis platform tool",
            hidden = true)
    private boolean platformTriggered;

    @CommandLine.Option(names = "--target-dir", description = "Target directory path")
    private String targetDir;

    @CommandLine.Option(names = "--scan-report", description = "Enable scan report generation")
    private boolean scanReport;

    @CommandLine.Option(names = "--list-rules",
            description = "List the rules available in the Ballerina scan tool.")
    private boolean listRules;

    @CommandLine.Option(names = "--include-rules", converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to include specific analysis issues")
    private List<String> includeRules = new ArrayList<>();

    @CommandLine.Option(names = "--exclude-rules", converter = StringToListConverter.class,
            description = "Specify the comma separated list of rules to exclude specific analysis issues")
    private List<String> excludeRules = new ArrayList<>();

    @CommandLine.Option(names = "--platforms", converter = StringToListConverter.class,
            description = "Specify the comma separated list of static code analysis platforms to report issues")
    private List<String> platforms = new ArrayList<>();

    public ScanCmd() {
        this.outputStream = System.out;
    }

    @Override
    public String getName() {
        return SCAN_COMMAND;
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

    @Override
    public void setParentCmdParser(CommandLine parentCmdParser) {
    }

    @Override
    public void execute() {
        if (helpFlag) {
            StringBuilder builder = helpMessage();
            outputStream.println(builder);
            return;
        }

        // Load the project
        Project project = getProject();

        // Stop further execution if project is null
        if (project == null) {
            return;
        }

        // Retrieve Scan.toml file configurations
        ScanTomlFile scanTomlFile = ScanUtils.retrieveScanTomlConfigurations(project);

        // Initialize project analyzer
        ProjectAnalyzer projectAnalyzer = new ProjectAnalyzer(scanTomlFile, outputStream);

        // Load and add rules from all static code analyzer plugins
        if (listRules) {
            List<Rule> allRules = projectAnalyzer.getExternalAnalyzerRules(project);
            allRules.addAll(InbuiltRules.INBUILT_RULES);
            ScanUtils.printRulesToConsole(allRules);
            return;
        }

        outputStream.println();
        outputStream.println("Running Scans...");

        // Get all rules to include from Scan.toml file
        Set<ScanTomlFile.RuleToFilter> rulesToInclude = scanTomlFile.getRulesToInclude();

        // Convert to String list
        List<String> allRulesToInclude = rulesToInclude.stream()
                .map(ScanTomlFile.RuleToFilter::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        // Add console defined rules to include
        allRulesToInclude.addAll(includeRules);

        // Get all rules to exclude from Scan.toml file
        Set<ScanTomlFile.RuleToFilter> rulesToExclude = scanTomlFile.getRulesToExclude();

        // Convert to String list
        List<String> allRulesToExclude = rulesToExclude.stream()
                .map(ScanTomlFile.RuleToFilter::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        // Add console defined rules to exclude
        allRulesToExclude.addAll(excludeRules);

        // Perform core scans on ballerina file/project
        List<Issue> issues = projectAnalyzer.analyzeProject(project);

        // Perform external scans
        List<Issue> externalIssues = projectAnalyzer.runExternalAnalyzers(project);

        // Add all issues
        issues.addAll(externalIssues);

        // Remove rules not in include list
        if (!allRulesToInclude.isEmpty()) {
            issues.removeIf(issue -> !allRulesToInclude.contains(issue.ruleId()));
        }

        // Remove rules in exclude list
        if (!allRulesToExclude.isEmpty()) {
            issues.removeIf(issue -> allRulesToExclude.contains(issue.ruleId()));
        }

        // Produce analysis results locally if 'local' platform is given
        if (platforms.isEmpty() && !platformTriggered) {
            // Print results to console
            ScanUtils.printToConsole(issues);

            // Generate reports only if scan is on a build project
            if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
                if (scanReport) {
                    Path scanReportPath;
                    outputStream.println();
                    outputStream.println("Generating scan report...");
                    if (targetDir != null) {
                        scanReportPath = ScanUtils.generateScanReport(issues, project, targetDir);
                    } else {
                        scanReportPath = ScanUtils.generateScanReport(issues, project, null);
                    }
                    outputStream.println();
                    outputStream.println("View scan report at:");
                    outputStream.println("\t" + ScanToolConstants.FILE_PROTOCOL + scanReportPath + "\n");
                }

                // Save results to directory
                Path reportPath;
                if (targetDir != null) {
                    reportPath = ScanUtils.saveToDirectory(issues, project, targetDir);
                } else {
                    reportPath = ScanUtils.saveToDirectory(issues, project, null);
                }

                outputStream.println();
                outputStream.println("View scan results at:");
                outputStream.println("\t" + reportPath + "\n");
            }
        }

        // Retrieve the platform JAR file paths and arguments from Scan.toml
        List<String> externalJarFilePaths = new ArrayList<>();
        Map<String, PlatformPluginContext> platformContexts = new HashMap<>();
        scanTomlFile.getPlatforms().forEach(platform -> {
            if (platformTriggered && platforms.size() == 1 && platforms.contains(platform.getName())) {
                externalJarFilePaths.add(platform.getPath());

                Map<String, String> platformArgs = platform.getArguments()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().toString()));

                platformContexts.put(platform.getName(), new PlatformPluginContextIml(
                        (HashMap<String, String>) platformArgs, platformTriggered));
            } else {
                platforms.add(platform.getName());
                externalJarFilePaths.add(platform.getPath());

                Map<String, String> platformArgs = platform.getArguments()
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, value -> value.getValue().toString()));

                platformContexts.put(platform.getName(), new PlatformPluginContextIml(
                        (HashMap<String, String>) platformArgs, platformTriggered));
            }
        });

        URLClassLoader ucl = loadExternalJars(externalJarFilePaths);

        ServiceLoader<StaticCodeAnalysisPlatformPlugin> scannerPlatformPlugins = ServiceLoader.load(
                StaticCodeAnalysisPlatformPlugin.class, ucl);

        // Proceed reporting to platforms if plugins exists
        scannerPlatformPlugins.forEach(staticCodeAnalysisPlatformPlugin -> {
            if (platforms.contains(staticCodeAnalysisPlatformPlugin.platform())) {
                PlatformPluginContext platformPluginContext = platformContexts.get(
                        staticCodeAnalysisPlatformPlugin.platform());

                outputStream.println("Reporting issues to " + staticCodeAnalysisPlatformPlugin.platform() + "...");
                staticCodeAnalysisPlatformPlugin.init(platformPluginContext);
                staticCodeAnalysisPlatformPlugin.onScan(issues);

                platforms.removeAll(Collections.singleton(staticCodeAnalysisPlatformPlugin.platform()));
            }
        });

        // If there are any platforms remaining which were not found in platform plugin JARs
        platforms.forEach(remainingPlatform -> {
            outputStream.println();
            outputStream.println("Error: The specified platform '" + remainingPlatform + "' is not available.");
            outputStream.println("Please ensure that the required platform plugin is installed and its path" +
                    " is correctly specified in Scan.toml.");
        });
    }

    public StringBuilder helpMessage() {
        InputStream inputStream = ScanCmd.class.getResourceAsStream("/cli-help/ballerina-scan.help");
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

    private Project getProject() {
        // retrieve the user passed argument or the current working directory
        if (argList.isEmpty()) {
            // Return the loaded project or the relevant error message
            try {
                return BuildProject.load(Paths.get(System.getProperty(ProjectConstants.USER_DIR)));
            } catch (RuntimeException e) {
                outputStream.println(e.getMessage());
                return null;
            }
        } else {
            // Return the loaded project or the relevant error message
            Path path = Paths.get(argList.get(0));
            try {
                if (path.toFile().isDirectory()) {
                    return BuildProject.load(path);
                } else {
                    return SingleFileProject.load(Paths.get(argList.get(0)));
                }
            } catch (RuntimeException e) {
                outputStream.println(e.getMessage());
                return null;
            }
        }
    }

    private URLClassLoader loadExternalJars(List<String> jarPaths) {
        List<URL> jarUrls = new ArrayList<>();
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
