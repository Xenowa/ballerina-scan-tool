package org.wso2.ballerina.plugin;

// Testing imports
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

// Sonar Plugin API based imports
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;

// Other imports
import java.util.Iterator;

class BallerinaSensorTest extends AbstractSensorTest {
    @AfterEach

    // ====
    // TODO
    // ====
    // Test has been commented out until a proper rule is created for ballerina
    // For now this is a dummy test to identify the minimum requirements required to run the Ballerina Plugin
    // Currently the Ballerina plugin does not perform any creation of AST and checking against rules
    // As the initial step this test will be used to get the plugin to report a issue to sonarqube
    // once a scan is performed
    // Next after the parsing logic is implemented this test will be modified to test if that works
    // Then this test will be modified further to check against one rule defined to see if the plugin is
    // able to actually parse ballerina files and report that one rule violation when a scan is performed
    @Test
    void test_one_rule() {
        // Simulating a Ballerina file in a project
        InputFile inputFile = createInputFileFromPath("sonar_bal_testing/main.bal");
        context.fileSystem().add(inputFile);

        // Setting up a dummy rule
        CheckFactory checkFactory = checkFactory("S107");
        BallerinaSensor sensor = sensor(checkFactory);
        sensor.execute(context);

        // Retrieving all issues sent by the sensor scan
        Iterator<Issue> issues = context.allIssues().iterator();

        // Checking if the sensor triggers the dummy rule
        Issue issue = issues.next();
        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S107");

        // Checking the location where the dummy rule was triggered from
        DefaultIssueLocation location = (DefaultIssueLocation) issue.primaryLocation();
        Assertions.assertThat(location.inputComponent()).isEqualTo(inputFile);
        Assertions.assertThat(location.message()).isEqualTo("This function has 8 parameters, which is greater than the 7 authorized.");
    }

    private BallerinaSensor sensor(CheckFactory checkFactory) {
        return new BallerinaSensor(checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(), language());
    }
}