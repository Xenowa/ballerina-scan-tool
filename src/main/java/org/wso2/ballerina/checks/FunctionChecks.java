package org.wso2.ballerina.checks;

import io.ballerina.tools.text.LineRange;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.rule.RuleKey;
import org.wso2.ballerinalang.compiler.tree.BLangFunction;
import org.wso2.ballerinalang.compiler.tree.BLangPackage;

import java.util.List;

import static org.wso2.ballerina.plugin.BallerinaPlugin.BALLERINA_REPOSITORY_KEY;

public class FunctionChecks {
    // First Ballerina Specific rule
    public static void tooManyParametersCheck(SensorContext context, InputFile inputFile, BLangPackage mainNode){
        // Obtain all functions from the syntax tree
        List<BLangFunction> functions = mainNode.getFunctions();

        // Only run the rule if the functions are not empty
        if(!functions.isEmpty()){
            functions.forEach(bLangFunction -> {
                // Only trigger the check if there are parameters and the count is greater than 7
                if(!bLangFunction.getParameters().isEmpty() && bLangFunction.getParameters().size() > 7){
                    reportIssue(context,
                            inputFile,
                            bLangFunction.getPosition().lineRange(),
                            "S107",
                            "This function has " + bLangFunction.getParameters().size() + " parameters, which is greater than the 7 authorized."
                            );
                }
            });
        }
    }

    public static void reportIssue(SensorContext context, InputFile inputFile, LineRange issueLocation, String ruleID, String message){
        // Creating the initial rule
        RuleKey ruleKey = RuleKey.of(BALLERINA_REPOSITORY_KEY, ruleID);

        // reporting the issue
        context.newIssue()
                .forRule(ruleKey)
                .at(context.newIssue()
                        .newLocation()
                        .on(inputFile)
                        .message(message)
                        .at(inputFile.newRange(
                                issueLocation.startLine().line(),
                                issueLocation.startLine().offset(),
                                issueLocation.endLine().line(),
                                issueLocation.endLine().offset()
                        ))
                )
                .save();
    }
}
