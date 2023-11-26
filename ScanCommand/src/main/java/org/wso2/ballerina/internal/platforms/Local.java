package org.wso2.ballerina.internal.platforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.SyntaxTree;

import org.wso2.ballerina.ExternalRules;
import org.wso2.ballerina.internal.ReportLocalIssue;
import org.wso2.ballerina.internal.StaticCodeAnalyzer;

import java.io.PrintStream;
import java.util.Map;
import java.util.ServiceLoader;

public class Local extends Platform {
    @Override
    public void scan(PrintStream outputStream) {
    }

    @Override
    public void scan(String userFile, PrintStream outputStream) {
        // TODO: Remake all the below functionality to:
        //  ===========================================
        //  - User should pass the ballerina project absolute path to the parseBallerinaProject() function
        //  - This should return back to the user a arraylist of BallerinaFiles named compiledBallerinaFiles
        //  - Each BallerinaFile object should have the following properties:
        //      - Absolute path of the Ballerina file
        //      - syntax tree
        //      - semantic model
        //  - The analysisIssues JsonArray Structure should be remade as follows:
        //      [
        //          {
        //              ballerinaFilePath: "Absolute path of ballerina file",
        //              reportedIssues: [
        //                  {
        //                      rule violation information ...
        //                  },
        //                  {
        //                      rule violation information ...
        //                  },
        //                  ...
        //              ]
        //          },
        //          {},
        //          ...
        //      ]
        //  - Next the BallerinaFiles should be iterated and the following should happen:
        //      - It should create an object that holds the absolute path of the ballerina file being scanned
        //      - The object should contain all issues related to the ballerina file being scanned
        //      - Each object should be finally added to the analysisIssues array

        // Set up the issue reporter here so that external issues also can be included
        ReportLocalIssue issueReporter = new ReportLocalIssue(analysisIssues);

        // parse the ballerina file
        Map<String, Object> compilation = parseBallerinaProject(userFile);

        // perform the static code analysis if the file was successfully parsed
        // Load all JAR's that implement the external rules interface (Using Java SPI)
        // This will only load classes from the JAR files located in the directory where
        // the JAR that triggers it is in
        ServiceLoader<ExternalRules> externalRulesJars = ServiceLoader.load(ExternalRules.class);
        // Iterate through the loaded interfaces
        for (ExternalRules externalRulesJar : externalRulesJars) {
            // Call the initialize method to trigger custom rule scans
            externalRulesJar.initialize(
                    (SyntaxTree) compilation.get("syntaxTree"),
                    (SemanticModel) compilation.get("semanticModel")
            );

            // Retrieve the externalIssues created by each custom rules JAR
            JsonArray externalIssuesArray = externalRulesJar.getExternalIssues();

            // Then report the external issues
            boolean successfullyReported = issueReporter.reportExternalIssues(externalIssuesArray);

            if (!successfullyReported) {
                handleParseIssue("Unable to load custom rules, issues reported are having invalid format!");
            }
        }

        scanWithSyntaxTree((SyntaxTree) compilation.get("syntaxTree"), issueReporter);

        // The semantic model will be used later when implementing complex rules
        // scanWithSemanticModel((SemanticModel) compilation.get("semanticModel"), outputStream);

        // Convert the JSON analysis results to the console
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(analysisIssues);
        outputStream.println(jsonOutput);
    }

    // For rules that can be implemented using the syntax tree model
    public void scanWithSyntaxTree(SyntaxTree syntaxTree, ReportLocalIssue issueReporter) {
        StaticCodeAnalyzer analyzer = new StaticCodeAnalyzer(syntaxTree);
        analyzer.initialize(issueReporter);
    }

    // For rules that can be implemented using the semantic model
    public void scanWithSemanticModel(SemanticModel semanticModel, PrintStream outputStream) {
        outputStream.println(semanticModel.toString());
    }
}
