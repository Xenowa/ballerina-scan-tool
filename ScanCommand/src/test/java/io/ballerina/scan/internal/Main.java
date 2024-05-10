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

package io.ballerina.scan.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ballerina.cli.launcher.CustomToolClassLoader;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Severity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final PrintStream outputStream = System.out;

    public static void main(String[] args) {
        try {
            readExternalRules();
        } catch (Exception e) {
            outputStream.println(e.getMessage());
        }
    }

    private static void readExternalRules() throws IOException {
        // Get the rules
        List<URL> urls = new ArrayList<>();
        urls.add(
                Path.of("C:\\Users\\Tharana Wanigaratne\\Desktop\\ballerina-scan-tool" +
                                "\\sample-custom-rules-analyzer\\build\\libs\\sample-custom-rules-analyzer-0.1.0.jar")
                        .toUri()
                        .toURL());
        CustomToolClassLoader cl = new CustomToolClassLoader(urls.toArray(new URL[0]), Main.class.getClassLoader());
        InputStream resourceAsStream = cl.getResourceAsStream("rules.json");

        // Read the rules
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream,
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        // Convert string to JSON Array
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray ruleArray = gson.fromJson(stringBuilder.toString(), JsonArray.class);

        // Generate rules from the array
        String org = "tharanawanigaratne";
        String name = "custom_rules_analyzer";
        List<Rule> rules = new ArrayList<>();

        ruleArray.forEach(rule -> {
            // Parse the rule objects
            JsonObject ruleObject = rule.getAsJsonObject();
            int numericId = ruleObject.get("id").getAsInt();
            Severity severity = switch (ruleObject.get("severity").getAsString()) {
                case "BUG" -> Severity.BUG;
                case "VULNERABILITY" -> Severity.VULNERABILITY;
                case "CODE_SMELL" -> Severity.CODE_SMELL;
                default -> null;
            };
            String description = ruleObject.get("description").getAsString();

            // Create in memory rule objects
            if (severity != null) {
                Rule inMemoryRule = RuleFactory.createRule(numericId, description, severity, org, name);
                rules.add(inMemoryRule);
            }
        });

        rules.forEach(rule -> {
            outputStream.println(gson.toJson(rule));
        });
    }
}
