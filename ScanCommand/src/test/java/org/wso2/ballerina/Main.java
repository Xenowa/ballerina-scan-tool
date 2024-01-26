package org.wso2.ballerina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.wso2.ballerina.internal.utilities.ScanTomlFile;

public class Main {
    public static void main(String[] args) {
        ScanTomlPropertiesManifest scanTomlPropertiesManifest = new ScanTomlPropertiesManifest();
        System.setProperty("ballerina.home", "C:\\Program Files\\Ballerina\\distributions\\ballerina-2201.8.2");
        ScanTomlFile scanTomlFile = scanTomlPropertiesManifest.retrieveScanToolConfigs("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scanTomlFile, ScanTomlFile.class);
        System.out.println(jsonOutput);
    }
}
