package org.wso2.ballerina;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.wso2.ballerina.internal.utilities.ScanTomlFile;
import org.wso2.ballerina.internal.utilities.ScanUtils;

public class Main {
    public static void main(String[] args) {
        // To set the ballerina home
        System.setProperty("ballerina.home", "C:\\Program Files\\Ballerina\\distributions\\ballerina-2201.8.5");

        ScanTomlFile scanTomlFile = ScanUtils.retrieveScanTomlConfigurations("C:\\Users\\Tharana Wanigaratne\\Desktop\\sonar-ballerina\\ScanCommand\\bal-scan-tool-tester");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scanTomlFile, ScanTomlFile.class);
        System.out.println(jsonOutput);
    }
}
