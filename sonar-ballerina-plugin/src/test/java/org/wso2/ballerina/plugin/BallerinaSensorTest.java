package org.wso2.ballerina.plugin;

// Testing imports

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// Sonar Plugin API based imports
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;

import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;
import org.sonar.api.config.internal.MapSettings;

class BallerinaSensorTest extends AbstractSensorTest {
    @AfterEach
    @Test
    void test_one_rule() {
        // Simulating Ballerina files in a project
        InputFile inputFile1 = createInputFileFromPath("sonar_bal_testing/main.bal");
        context.fileSystem().add(inputFile1);
        InputFile inputFile2 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "credentialsNotHardCoded/credentialsNotHardCoded.bal");
        context.fileSystem().add(inputFile2);
        InputFile inputFile3 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "databaseInjection/databaseInjection.bal");
        context.fileSystem().add(inputFile3);
        InputFile inputFile4 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "emptyFunction/emptyFunction.bal");
        context.fileSystem().add(inputFile4);
        InputFile inputFile5 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "functionPathInjection/functionPathInjection.bal");
        context.fileSystem().add(inputFile5);
        InputFile inputFile6 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "insecureEndpoint/insecureEndpoint.bal");
        context.fileSystem().add(inputFile6);
        InputFile inputFile7 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "panicChecker/panicChecker.bal");
        context.fileSystem().add(inputFile7);
        InputFile inputFile8 = createInputFileFromPath("sonar_bal_testing/modules/" +
                "tooManyParameters/tooManyParameters.bal");
        context.fileSystem().add(inputFile8);

        // Setting the location where the report is saved after performing a bal scan
        MapSettings settings = context.settings();
        settings.appendProperty("analyzedResultsPath", "C:\\Users\\Tharana Wanigaratne\\Desktop\\" +
                "sonar-ballerina\\sonar-ballerina-plugin\\src\\test\\java\\org\\wso2\\ballerina\\" +
                "plugin\\sonar_bal_testing\\ballerina-analysis-results.json");

        // Setting up a dummy rule
        CheckFactory checkFactory = checkFactory("S107", "S108", "S109");
        BallerinaSensor sensor = sensor(checkFactory);

        sensor.execute(context);
        context.allIssues().forEach(issue -> {
            if (!(issue.ruleKey() == null)) {
                System.out.println(issue.ruleKey().rule());
                DefaultIssueLocation location = (DefaultIssueLocation) issue.primaryLocation();
                System.out.println(location.message());
            }
        });

//        // Retrieving all issues sent by the sensor scan
//        Iterator<Issue> issues = context.allIssues().iterator();
//
//        // Checking if the sensor triggers the dummy rule
//        Issue issue = issues.next();
//        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S107");
//
//        // Checking the location where the dummy rule was triggered from
//        DefaultIssueLocation location = (DefaultIssueLocation) issue.primaryLocation();
//        Assertions.assertThat(location.inputComponent()).isEqualTo(inputFile);
//        Assertions.assertThat(location.message()).isEqualTo("This function has 8 parameters, which is greater than the 7 authorized.");
    }

    private BallerinaSensor sensor(CheckFactory checkFactory) {
        return new BallerinaSensor(checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(), language());
    }
}