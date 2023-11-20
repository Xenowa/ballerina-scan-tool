package org.wso2.ballerina.internal;

import io.ballerina.cli.BLauncherCmd;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.wso2.ballerina.internal.platforms.CodeQL;
import org.wso2.ballerina.internal.platforms.Local;
import org.wso2.ballerina.internal.platforms.Platform;
import org.wso2.ballerina.internal.platforms.SemGrep;
import org.wso2.ballerina.internal.platforms.SonarQube;
import org.wso2.ballerina.internal.miscellaneous.sonarqubeold.SonarQubeOld;
import picocli.CommandLine;

@CommandLine.Command(name = "scan", description = "Perform static code analysis for ballerina packages")
public class ScanCommand implements BLauncherCmd {
    // CMD Launcher Attributes
    private final PrintStream outputStream; // for success outputs
    private final PrintStream errorStream; // for error outputs

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden = true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"--platform"}, description = "static code analysis output platform", defaultValue = "local")
    private String platform;

    @CommandLine.Option(names = {"--rule"}, description = "single rule to be checked during the static code analysis", defaultValue = "all")
    public static String userRule;

    public ScanCommand() {
        this.outputStream = System.out;
        this.errorStream = System.err;
    }

    public ScanCommand(PrintStream outputStream) {
        this.outputStream = outputStream;
        this.errorStream = outputStream;
    }

    public String checkPath() {
        // if invalid number of arguments are passed to the bal scan command
        if (this.argList.size() != 1) {
            this.outputStream.println("Invalid number of arguments received!\n try bal scan --help for more information.");
            return "";
        }

        // retrieve the user passed argument
        String userFilePath = this.argList.get(0); // userFile

        // check if the user passed file is a ballerina file or not
        String[] userFileExtension = userFilePath.split("\\.(?=[^\\.]+$)"); // [userFile, bal]
        if ((userFileExtension.length != 2) || !userFileExtension[1].equals("bal")) {
            this.outputStream.println("Invalid file format received!\n file format should be of type '.bal'");
            return "";
        }

        // check if such ballerina file exists in the working directory
        File file = new File(userFilePath);
        if (!file.exists()) {
            this.outputStream.println("No such file exists!\n please check the file name and then re run the command");
            return "";
        }

        // return name of user file if it exists
        return userFilePath;
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

        // if the rule to scan is passed by the user
        boolean rulesAreActivated;
        if (!userRule.equals("all")) {
            rulesAreActivated = InbuiltRules.activateUserDefinedRule(userRule);
        } else {
            rulesAreActivated = true;
        }

        // proceed with the scanning only if all the rules are activated
        if (!rulesAreActivated) {
            outputStream.println("The provided rule is invalid, please run bal scan --help for more info!");
            return;
        }

        // Set the trigger platform depending on user input
        Platform triggerPlatform;
        switch (platform) {
            case "sonarqube" -> triggerPlatform = new SonarQube();
            case "sonarqubeold" -> triggerPlatform = new SonarQubeOld();
            case "codeql" -> triggerPlatform = new CodeQL();
            case "semgrep" -> triggerPlatform = new SemGrep();
            case "local" -> triggerPlatform = new Local();
            default -> triggerPlatform = null;
        }

        // proceed to retrieve the user provided filepath to perform a scan on if the platform was local
        String userFilePath;
        if (platform.equals("local")) {
            userFilePath = checkPath();
            if (userFilePath.equals("")) {
                return;
            }

            // execute local scanner
            triggerPlatform.scan(userFilePath, outputStream);
        } else if (triggerPlatform != null) {
            userFilePath = validateEmptyPath();
            if (!userFilePath.equals("")) {
                return;
            }

            // execute relevant scanner if a valid platform is provided
            triggerPlatform.scan(outputStream);
        } else {
            outputStream.println("Platform provided is invalid, run bal scan --help for more info!");
        }
    }

    // INFO Methods
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
}
