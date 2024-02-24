package io.ballerina.scan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.scan.utilities.ScanUtils;

import java.io.PrintStream;

public class Main {

    private static final PrintStream outputStream = System.out;

    public static void main(String[] args) {
        // To set the ballerina home
        System.setProperty("ballerina.home", "C:\\Program Files\\Ballerina\\distributions\\ballerina-2201.8.5");

        ScanTomlFile scanTomlFile = ScanUtils.retrieveScanTomlConfigurations("C:\\Users" +
                "\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scanTomlFile, ScanTomlFile.class);
        outputStream.println(jsonOutput);
    }
}
