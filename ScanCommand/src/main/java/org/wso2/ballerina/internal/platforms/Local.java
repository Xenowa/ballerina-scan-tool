package org.wso2.ballerina.internal.platforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import org.wso2.ballerina.ExternalRules;

import static org.wso2.ballerina.internal.InbuiltRules.INBUILT_RULES;

import org.wso2.ballerina.internal.ReportLocalIssue;
import org.wso2.ballerina.internal.StaticCodeAnalyzer;
import org.wso2.ballerina.internal.miscellaneous.FunctionChecks;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class Local extends Platform {
    @Override
    public void scan(PrintStream outputStream) {
    }

    @Override
    public void scan(String userFile, PrintStream outputStream) {
        // Set up the issue reporter here so that external issues also can be included
        ReportLocalIssue issueReporter = new ReportLocalIssue(analysisIssues);

        // parse the ballerina file
        Map<String, Object> compilation = parseBallerinaProject(userFile);

        // perform the static code analysis if the file was successfully parsed
        if (compilation != null) {
            // ======================
            // Accessing Custom Rules
            // ======================
            // For now try to access custom rules from 1 JAR
            String CUSTOM_PLUGIN_JAR_PATH = "C:/Users/Tharana Wanigaratne/Desktop" +
                    "/sonar-ballerina/custom-rules-plugin/build/libs/custom-rules-plugin-1.0-all.jar";
            File customRulesJarFile = new File(CUSTOM_PLUGIN_JAR_PATH);
            if (customRulesJarFile.exists()) {
                boolean success = false;

                // Retrieve the Object and the relevant methods and attributes to perform operations
                try {
                    URLClassLoader classLoader = new URLClassLoader(new URL[]{customRulesJarFile.toURI().toURL()});
                    // As it's extending an abstract class we have to give the concrete class to work with it
                    Class<?> externalRulesClass = classLoader.loadClass("org.wso2.ballerina.plugin.CustomBallerinaPlugin");

                    // Create a new instance of the object
                    Object externalRulesObject = externalRulesClass.getDeclaredConstructor().newInstance();

                    // Initialize the custom rules
                    Method setCustomRules = externalRulesClass.getDeclaredMethod("setCustomRules");
                    setCustomRules.setAccessible(true);
                    setCustomRules.invoke(externalRulesObject);

                    // Retrieve the custom rules
                    Method getCustomRules = externalRulesClass.getDeclaredMethod("getCustomRules");
                    getCustomRules.setAccessible(true);
                    ArrayList<String> customRulesList = (ArrayList<String>) getCustomRules.invoke(externalRulesObject);

                    // Load the custom rules
                    success = loadCustomRules(customRulesList);


                    // if the custom rule loading was successful
                    if (!success) {
                        handleParseIssue("Unable to load custom rules, continuing to scan with inbuilt rules!");
                    } else {
                        // if loading custom rules was successfully
                        // First call the initialize method
                        Method initialize = externalRulesClass.getDeclaredMethod("initialize",
                                SyntaxTree.class,
                                SemanticModel.class);
                        initialize.setAccessible(true);
                        Object[] args = new Object[2];
                        args[0] = (SyntaxTree) compilation.get("syntaxTree");
                        args[1] = (SemanticModel) compilation.get("semanticModel");
                        initialize.invoke(externalRulesObject, args);

                        // Next retrieve the external issues created by the initialize method of the custom rules plugin
                        Method externalIssues = externalRulesClass.getDeclaredMethod("getExternalIssues");
                        externalIssues.setAccessible(true);
                        JsonArray externalIssuesArray = (JsonArray) externalIssues.invoke(externalRulesObject);

                        // Then report the external issues
                        boolean successfullyReported = issueReporter.reportExternalIssues(externalIssuesArray);

                        if (!successfullyReported) {
                            handleParseIssue("Unable to load custom rules, issues reported are having invalid format!");
                        }
                    }

                } catch (MalformedURLException | ClassNotFoundException | InstantiationException |
                         IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    handleParseIssue("Error!, " + e);
                }
            }

            scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"), issueReporter);

            // Deprecated scan with syntax tree
            // scanWithSyntaxTreeOld((SyntaxTree) compilation.get("syntaxTree"));

            // The semantic model will be used later when implementing complex rules
            // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);
        }

        // Convert the JSON analysis results to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(analysisIssues);
        outputStream.println(jsonOutput);
    }

    // For loading custom rules
    public boolean loadCustomRules(ArrayList<String> customRules) {
        // Check if custom rules are same as inbuilt rules
        AtomicBoolean customRulesMatchInbuiltRules = new AtomicBoolean(false);
        customRules.forEach(rule -> {
            if (INBUILT_RULES.containsKey(rule)) {
                customRulesMatchInbuiltRules.set(true);
            } else {
                // Add custom rules if not already available
                INBUILT_RULES.put(rule, true);
            }
        });

        return !customRulesMatchInbuiltRules.get();
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree, ReportLocalIssue issueReporter) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize(issueReporter);
    }

    public void scanWithSyntaxTreeOld(SyntaxTree syntaxTree) {
        // Function relaated visits
        FunctionChecks functionChecks = new FunctionChecks(syntaxTree);
        functionChecks.initialize();
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
    }
}
