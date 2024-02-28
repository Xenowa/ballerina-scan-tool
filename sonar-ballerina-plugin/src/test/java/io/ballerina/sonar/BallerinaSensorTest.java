/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.sonar;

// Testing imports

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;

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
//        MapSettings settings = context.settings();
//        settings.appendProperty("analyzedResultsPath", "C:\\Users\\Tharana Wanigaratne\\Desktop" +
//                "\\sonar-ballerina\\sonar-ballerina-plugin\\src\\test\\java\\io\\ballerina\\sonar" +
//                "\\sonar_bal_testing\\ballerina-analysis-results.json");

        // Setting up a dummy rule
        CheckFactory checkFactory = checkFactory("S107", "S108");
        BallerinaSensor sensor = sensor(checkFactory);

        sensor.execute(context);

//        // Retrieve the first issue reported to the sensor context
//        Issue issue = context.allIssues().iterator().next();
//        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S108");
//        Assertions.assertThat(issue.primaryLocation().textRange().start().line()).isEqualTo(1);
//        Assertions.assertThat(issue.primaryLocation().textRange().start().lineOffset()).isEqualTo(28);
//        Assertions.assertThat(issue.primaryLocation().textRange().end().line()).isEqualTo(3);
//        Assertions.assertThat(issue.primaryLocation().textRange().end().lineOffset()).isEqualTo(1);
    }

    private BallerinaSensor sensor(CheckFactory checkFactory) {

        return new BallerinaSensor(checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(), language());
    }
}
