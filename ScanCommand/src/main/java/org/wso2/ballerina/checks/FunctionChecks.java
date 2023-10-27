package org.wso2.ballerina.checks;

import io.ballerina.tools.text.LineRange;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.io.PrintStream;
import java.util.List;

import static org.wso2.ballerina.SonarQubeScanner.CHECK_VIOLATION;

public class FunctionChecks {
    // First Ballerina Specific rule
    public static void tooManyParametersCheck(BLangPackage mainNode, PrintStream outputStream){
        // Obtain all functions from the syntax tree
        List<BLangFunction> functions = mainNode.getFunctions();

        // Only run the rule if the functions are not empty
        if(!functions.isEmpty()){
            functions.forEach(bLangFunction -> {
                // Only trigger the check if there are parameters and the count is greater than 7
                if(!bLangFunction.getParameters().isEmpty() && bLangFunction.getParameters().size() > 7){
                    reportIssue(
                            CHECK_VIOLATION,
                            bLangFunction.getPosition().lineRange(),
                            "S107",
                            "This function has " + bLangFunction.getParameters().size() + " parameters, which is greater than the 7 authorized.",
                            outputStream
                            );
                }
            });
        }
    }

    public static void reportIssue(String issueType, LineRange issueLocation, String ruleID, String message, PrintStream outputStream){
        // For now we will be displaying the rule violation output directly to the console
        outputStream.println("Displaying Static Code Analysis results:");
        outputStream.println("Issue Type: " + issueType);
        outputStream.println("Start Line: " + issueLocation.startLine().line());
        outputStream.println("Start Line Offset: " + issueLocation.startLine().offset());
        outputStream.println("End Line: " + issueLocation.endLine().line());
        outputStream.println("End Line Offset: " + issueLocation.endLine().offset());
        outputStream.println("Rule ID: " + ruleID);
        outputStream.println(message);


        // What we can and need to send from the bal scan tool side:
        // 1. Issue type (custom - to determine which reportIssue method should be called in the SonarScanner side):
        // i.e: CHECK_VIOLATION -> for violating rules during the scan
        // i.e: SOURCE_INVALID -> if the provided file is not scannable
        // 2. lineRange: should be sent in it's atomic form as follows
        // LineRange issueLocation = bLangFunction.getPosition().lineRange()
        //  int startLine = issueLocation.startLine().line()
        //  int startLineOffset = issueLocation.startLine().offset(),
        //  int endLine = issueLocation.endLine().line(),
        //  int endLineOffset = issueLocation.endLine().offset()
        // 3. ruleID : S107
        // 4. message: ""This function has " + bLangFunction.getParameters().size() + " parameters, which is greater than the 7 authorized.""
        // We should ideally send a GSON JSON output to the outputStream

        // The following properties should be set up from the SonarQube plugin side
        // 1. SensorContext context
        // 2. InputFile inputFile

        // Final reporting code from the SonarQube Plugin side should be something like the following
        /*
        * // GSON Object that has all bal scan outputs named "balScanOutput"
        * RuleKey ruleKey = RuleKey.of(BALLERINA_REPOSITORY_KEY, balScanOutput.ruleID);
        * context.newIssue()
                .forRule(ruleKey)
                .at(context.newIssue()
                        .newLocation()
                        .on(inputFile)
                        .message(balScanOutput.message)
                        .at(inputFile.newRange(
                                balScanOutput.startLine(),
                                balScanOutput.startLineOffset(),
                                balScanOutput.endLine(),
                                balScanOutput.endLineOffset(),
                        ))
                )
                .save();
        * */
    }
}
