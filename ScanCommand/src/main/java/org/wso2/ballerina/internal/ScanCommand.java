package org.wso2.ballerina.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.cli.BLauncherCmd;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.wso2.ballerina.Platform;
import org.wso2.ballerina.internal.platforms.Local;
import picocli.CommandLine;

@CommandLine.Command(name = "scan", description = "Perform static code analysis for ballerina packages")
public class ScanCommand implements BLauncherCmd {
    // =============================
    // Ballerina Launcher Attributes
    // =============================
    private final PrintStream outputStream; // for success outputs
    private final PrintStream errorStream; // for error outputs

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"--platform"},
            description = "static code analysis output platform",
            defaultValue = "local")
    private String platform;

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
    public String checkPath() {
        // if invalid number of arguments are passed to the bal scan command
        boolean tooManyArguments = this.argList.size() > 1;
        if (tooManyArguments) {
            this.outputStream.println("Invalid number of arguments received!\n" +
                    "run bal scan --help for more information.");
            return "";
        }

        // boolean to check if there are any arguments passed
        boolean isPathProvided = this.argList.size() == 1;

        // retrieve the user passed argument or the current working directory
        String userFilePath = isPathProvided ? this.argList.get(0) : System.getProperty("user.dir");

        // Check if the user provided path is a file or a directory
        File file = new File(userFilePath);
        if (file.exists()) {
            if (file.isFile()) {
                // Check if the file extension is '.bal'
                if (!userFilePath.endsWith(".bal")) {
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
                File ballerinaTomlFile = new File(userFilePath, "Ballerina.toml");
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

    public String validateEmptyPath() {
        if (!this.argList.isEmpty()) {
            this.outputStream.println("arguments are invalid!,\n try bal scan --help for more information.");
            return "invalidEntry";
        }
        return "";
    }

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
        switch (platform) {
            case "local" -> {
                Local localPlatform = new Local();
                // proceed to retrieve the user provided filepath to perform a scan on if the platform was local
                String userPath;
                userPath = checkPath();
                if (userPath.equals("")) {
                    return;
                }

                // Perform scan on ballerina file/project
                JsonArray scannedResults = localPlatform.analyzeProject(Path.of(userPath));

                // Stop reporting if there are no files analyzed
                if (scannedResults.isEmpty()) {
                    outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
                    return;
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonOutput = gson.toJson(scannedResults);
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
                JsonArray scannedResults = localPlatform.analyzeProject(Path.of(userPath));

                // Stop reporting if there are no files analyzed
                if (scannedResults.isEmpty()) {
                    outputStream.println("ballerina: The source file '" + userPath + "' belongs to a Ballerina package.");
                    return;
                }

                // Convert the output to a string
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonOutput = gson.toJson(scannedResults);

                // Platform plugins for reporting
                ServiceLoader<Platform> platforms = ServiceLoader.load(Platform.class);
                // Iterate through the loaded interfaces
                for (Platform platform : platforms) {
                    // Retrieve the platform name through initialize method
                    String platformName = platform.initialize();

                    // If a valid platform name is provided then trigger reporting
                    // For now we will make the initialize return null in other platforms
                    if (platformName != null) {
                        // Pass the output and the streams to see the execution results
                        platform.onScan(jsonOutput, outputStream, errorStream);
                    }
                }
            }
            case "codeql", "semgrep" -> {
                outputStream.println("Platform support is not available yet!");
            }
            default -> outputStream.println("Platform provided is invalid, run bal scan --help for more info!");
        }
    }
}
