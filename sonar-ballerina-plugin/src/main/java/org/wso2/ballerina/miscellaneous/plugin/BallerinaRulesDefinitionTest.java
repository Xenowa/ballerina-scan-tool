package org.wso2.ballerina.miscellaneous.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;
import org.wso2.ballerina.plugin.BallerinaPlugin;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BallerinaRulesDefinitionTest implements RulesDefinition {

    private static final String RESOURCE_FOLDER = "org/sonar/l10n/ballerina/rules/ballerina";
    private final SonarRuntime runtime;

    public BallerinaRulesDefinitionTest(SonarRuntime runtime) {
        this.runtime = runtime;
    }

    // Adding custom rules
    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(BallerinaPlugin.BALLERINA_REPOSITORY_KEY, BallerinaPlugin.BALLERINA_LANGUAGE_KEY);
        repository.setName(BallerinaPlugin.REPOSITORY_NAME);

        // For adding custom rules:
        // 1 we have to call bal scan rules, and obtain a JSON arraylist of rules
        // 2 then we have to generate the html and JSON files matching the JSON objects retrieved
        // 3 The temporary folder location where these recide needs to be provided as well
        // Then we can sucessfully load custom rules to be displayed in the SonarQube dashboard
        // Simulate a single rule Object retrieved from the JSON array
        JsonObject customRule = new JsonObject();
        customRule.addProperty("title", "function with empty body");
        customRule.addProperty("type", "CODE_SMELL");
        customRule.addProperty("status", "ready");
        JsonObject remediation = new JsonObject();
        remediation.addProperty("func", "Constant\\/Issue");
        remediation.addProperty("constantCost", "10min");
        customRule.add("remediation", remediation);
        JsonArray tags = new JsonArray();
        tags.add("dead-code");
        customRule.add("tags", tags);
        customRule.addProperty("defaultSeverity", "Major");
        customRule.addProperty("ruleSpecification", "RSPEC-107");
        customRule.addProperty("sqKey", "S109");
        customRule.addProperty("scope", "All");
        customRule.addProperty("quickfix", "unknown");
        customRule.addProperty("htmlContent", "Add a nested comment explaining why this function is empty" +
                "or complete the implementation.");

        try {
            // Create a custom file location
            Path tempDirectory = Files.createTempDirectory("ballerina");

            // filter out the content required to create the JSON file
            JsonObject newCustomRule = new JsonObject();
            newCustomRule.addProperty("title", customRule.get("title").getAsString());
            newCustomRule.addProperty("type", customRule.get("type").getAsString());
            newCustomRule.addProperty("status", customRule.get("status").getAsString());
            newCustomRule.add("remediation", customRule.get("remediation").getAsJsonObject());
            newCustomRule.add("tags", customRule.get("tags").getAsJsonArray());
            newCustomRule.addProperty("defaultSeverity", customRule.get("defaultSeverity").getAsString());
            newCustomRule.addProperty("ruleSpecification", customRule.get("ruleSpecification").getAsString());
            newCustomRule.addProperty("sqKey", customRule.get("sqKey").getAsString());
            newCustomRule.addProperty("scope", customRule.get("scope").getAsString());
            newCustomRule.addProperty("quickfix", customRule.get("quickfix").getAsString());

            // Create a JSON file in the temporary directory
            Path jsonFilePath = tempDirectory.resolve(customRule.get("sqKey").getAsString() + ".json");

            // Write the JSON content to the file
            try (FileWriter fileWriter = new FileWriter(jsonFilePath.toFile())) {
                fileWriter.write(newCustomRule.toString());
                fileWriter.flush();
            }

            // Create an HTML file in the temporary directory
            Path htmlFilePath = tempDirectory.resolve(customRule.get("sqKey").getAsString() + ".html");
            try (FileWriter fileWriter = new FileWriter(htmlFilePath.toFile())) {
                fileWriter.write("<h1>" + customRule.get("htmlContent").getAsString() + "</h1>");
                fileWriter.flush();
            }

            // Create the Sonar_way_profile.json file in temporary directory
            // Create the JSON structure using JsonObject and JsonArray
            JsonObject sonarConfig = new JsonObject();
            sonarConfig.addProperty("name", "Sonar way");

            JsonArray ruleKeys = new JsonArray();
            ruleKeys.add("S107");
            ruleKeys.add("S108");
            ruleKeys.add(customRule.get("sqKey"));

            sonarConfig.add("ruleKeys", ruleKeys);
            Path rulesListJsonFilePath = tempDirectory.resolve("Sonar_way_profile.json");
            try (FileWriter fileWriter = new FileWriter(rulesListJsonFilePath.toFile())) {
                fileWriter.write(sonarConfig.toString());
                fileWriter.flush();
            }

            // Setting up adding the rules by keys instead of classes for testing purposes
            // NOTE: Rule number 001 is not valid
            List<String> BALLERINA_CHECKS = new ArrayList<>();
            BALLERINA_CHECKS.add("S107");
            BALLERINA_CHECKS.add("S108");
            BALLERINA_CHECKS.add(customRule.get("sqKey").toString());

            // load the content of temp directory to sonarqube server
            RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(
                    tempDirectory.toAbsolutePath().toString(),
                    rulesListJsonFilePath.toAbsolutePath().toString(),
                    runtime);

            ruleMetadataLoader.addRulesByRuleKey(repository, BALLERINA_CHECKS);
            repository.done();

            // Delete the temporary directory with its files
            Files.deleteIfExists(tempDirectory);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}