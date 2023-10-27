package balToolTester;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.tools.text.LineRange;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    private static PrintStream outputStream = System.out;
    private static PrintStream errorStream = System.err;

    public static void printFileContents(Path filePath){
        String fileContent = null;
        try{
            fileContent = new String(Files.readAllBytes(filePath)).trim();
        }catch (IOException exception){
            throw new RuntimeException(exception);
        }

        outputStream.println(fileContent);
    }

    public static void testBalTool(Path filePath){
        String absolutePath = filePath.toAbsolutePath().toString();
        // Build a process to run the bal tool
        ProcessBuilder fileScan = new ProcessBuilder("cmd","/c", "bal", "scan", "--platform=sonarqube", absolutePath);
        try {
            // Start the process
            Process process = fileScan.start();

            // Read the output of the process into a string
            InputStream scanProcessInput = process.getInputStream();
            Scanner scanner = new Scanner(scanProcessInput).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";

            // Parse the object into a JSON object
            JsonParser jsonParser = new JsonParser();
            JsonObject balScanOutput = jsonParser.parse(output).getAsJsonObject();
            outputStream.println(balScanOutput.get("issueType"));
            outputStream.println(balScanOutput.get("ruleID"));
            outputStream.println(balScanOutput.get("message"));
            outputStream.println(balScanOutput.get("startLine"));
            outputStream.println(balScanOutput.get("startLineOffset"));
            outputStream.println(balScanOutput.get("endLine"));
            outputStream.println(balScanOutput.get("endLineOffset"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {
        Path balFilePath = Path.of("ScanCommand/bal-scan-tool-tester/main.bal");
        // printFileContents(balFilePath);
        testBalTool(balFilePath);
    }
}