package org.wso2.ballerina;
import io.ballerina.cli.BLauncherCmd;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine;

@CommandLine.Command(name = "scan", description = "Perform static code analysis for ballerina packages")
public class ScanCommand implements BLauncherCmd {
    // CMD Launcher Attributes
    private final PrintStream outStream; // for success outputs
    private final PrintStream errorStream; // for error outputs

    @CommandLine.Parameters(description = "Program arguments")
    private final List<String> argList = new ArrayList<>();

    @CommandLine.Option(names = {"--help", "-h", "?"}, hidden=true)
    private boolean helpFlag;

    @CommandLine.Option(names = {"--platform"}, description = "static code analysis output platform")
    private String platform;

    public ScanCommand() {
        this.outStream = System.out;
        this.errorStream = System.err;
    }

    public ScanCommand(PrintStream outStream) {
        this.outStream = outStream;
        this.errorStream = outStream;
    }

    public String checkPath(){
        // if an invalid argument is passed to the bal scan command
        if (this.argList == null || this.argList.size() != 1) {
            this.outStream.println("Invalid number of arguments received!\n try bal scan --help for more information.");
            return "";
        }

        // retrieve the user passed argument
        String userFilePath = this.argList.get(0); // userFile

        // check if the user passed file is a ballerina file or not
        String[] userFileExtension = userFilePath.split("\\.(?=[^\\.]+$)"); // [userFile, bal]
        if((userFileExtension.length != 2) || !userFileExtension[1].equals("bal")){
            this.outStream.println("Invalid file format received!\n file format should be of type '.bal'");
            return "";
        }

        // check if such ballerina file exists in the working directory
        File file = new File(userFilePath);
        if (!file.exists()) {
            this.outStream.println("No such file exists!\n please check the file name and then re run the command");
            return "";
        }

        // return name of user file if it exists
        return userFilePath;
    }

    public void sonarScan(String userFile){
        this.outStream.println("sonarqube static code analysis successful!");
        // TODO: Set up functionality to get access to the Ballerina Semantic Model
        // TODO: Implement one rule to check against
        // TODO: send the analysis report info to the prinstream
    }

    // MAIN method
    @Override
    public void execute(){
        // if bal scan --help is passed
        if (this.helpFlag) {
            StringBuilder builder = new StringBuilder();
            builder.append("Tool for providing static code analysis results for Ballerina projects\n\n");
            builder.append("bal scan --platform=<option> <ballerina-file>\n\n");
            builder.append("--option--\n");
            builder.append("\toption 1: sonarqube\n");
            builder.append("\toption 2: codeql\n");
            builder.append("\toption 3: semgrep\n\n");
            builder.append("i.e: bal scan --platform=sonarqube balFileName.bal\n");
            this.outStream.println(builder);
            return;
        }

        // check for user provided file path is in the directory
        String userFilePath = checkPath();
        if(userFilePath.equals("")){
            return;
        }

        // --platform=<option>
        switch (platform){
            case "sonarqube":
                sonarScan(userFilePath);
                break;
            case "codeql":
            case "semgrep":
                this.outStream.println("Platform support is not available yet!");
                break;
            default:
                this.outStream.println("Entered platform is invalid!, please run bal scan --help for more info");
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
