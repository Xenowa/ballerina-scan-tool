package org.wso2.ballerina.internal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.projects.Project;
import io.ballerina.projects.ProjectKind;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.projects.internal.model.Target;
import org.wso2.ballerina.Issue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanUtils {
    public static void printToConsole(ArrayList<Issue> issues, PrintStream outputStream) {
        String jsonOutput = convertIssuesToJsonString(issues);

        outputStream.println();
        outputStream.println(jsonOutput);
    }

    public static String convertIssuesToJsonString(ArrayList<Issue> issues) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray issuesAsJson = gson.toJsonTree(issues).getAsJsonArray();
        return gson.toJson(issuesAsJson);
    }

    public static Path saveToDirectory(ArrayList<Issue> issues, String projectPath, String directoryName) {
        // Create folder to save issues to
        Project project = ProjectLoader.loadProject(Path.of(projectPath));
        Target target;
        try {
            if (directoryName != null) {
                String file = project.sourceRoot().toString() + ScanToolConstants.PATH_SEPARATOR + directoryName;
                Path tempDirectory = Files.createDirectory(Path.of(file));
                target = new Target(tempDirectory);
            } else if (project.kind() == ProjectKind.BUILD_PROJECT) {
                target = new Target(project.targetDir());
            } else {
                Path tempDirectory = Files.createTempDirectory(ScanToolConstants.TARGET_DIR_NAME
                        + System.nanoTime());
                target = new Target(tempDirectory);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Retrieve path where report is saved
        Path jsonFilePath;
        try {
            Path reportPath = target.getReportPath();

            // Convert the issues to a json string
            String jsonOutput = convertIssuesToJsonString(issues);

            // Create the file to save the analysis issues to
            File jsonFile = new File(reportPath.resolve(ScanToolConstants.RESULTS_JSON_FILE).toString());

            // Write results to file and return saved file path
            try (FileOutputStream fileOutputStream = new FileOutputStream(jsonFile)) {
                try (Writer writer = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8)) {
                    writer.write(new String(jsonOutput.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                    writer.close();

                    jsonFilePath = jsonFile.toPath();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return jsonFilePath;
    }

    // Save scan results in the HTML template
    public static String generateScanReport() {
        return null;
    }

    public static void printRulesToConsole(HashMap<String, Rule> rules, PrintStream outputStream) {
        outputStream.println("Default available rules:");

        outputStream.println("\t" + "RuleID" + "\t"
                + " | " + "Rule Activated" + "\t"
                + " | " + "Rule Description" + "\n" + "\t"
                + "---------------------------------------------------");

        rules.forEach((ruleID, rule) -> {
            outputStream.println("\t" + ruleID + "\t"
                    + " | " + rule.ruleIsActivated()
                    + (rule.ruleIsActivated() ? "\t" + "\t" + "\t" : "\t" + "\t")
                    + " | " + rule.getRuleDescription());
        });

        outputStream.println();
    }

    public static boolean activateUserDefinedRule(Map<String, Rule> rules, List<String> userDefinedRules) {
        AtomicBoolean userDefinedRulesActivated = new AtomicBoolean(true);

        // Disable all inbuilt rules
        rules.values().forEach(inbuiltRule -> {
            inbuiltRule.setRuleIsActivated(false);
        });

        userDefinedRules.forEach(userDefinedRule -> {
            // If even a single user defined rule does not exist in the inbuilt rules return false
            if (!rules.containsKey(userDefinedRule)) {
                userDefinedRulesActivated.set(false);
            } else {
                rules.get(userDefinedRule).setRuleIsActivated(true);
            }
        });

        return userDefinedRulesActivated.get();
    }
}
