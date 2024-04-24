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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.sensor.issue.internal.DefaultNoSonarFilter;

import java.nio.file.Files;
import java.nio.file.Paths;

class BallerinaSensorTest extends AbstractSensorTest {

    private static final String ISSUES_FILE_PATH = "ballerina-analysis-results.json";

    @AfterEach
    @Test
    void test_one_rule() {
        // Simulating Ballerina files in a project
        InputFile inputFile1 = createInputFileFromPath("main.bal");
        context.fileSystem().add(inputFile1);
        InputFile inputFile2 = createInputFileFromPath("modules/" +
                "credentialsNotHardCoded/credentialsNotHardCoded.bal");
        context.fileSystem().add(inputFile2);
        InputFile inputFile3 = createInputFileFromPath("modules/" +
                "databaseInjection/databaseInjection.bal");
        context.fileSystem().add(inputFile3);
        InputFile inputFile4 = createInputFileFromPath("modules/" +
                "emptyFunction/emptyFunction.bal");
        context.fileSystem().add(inputFile4);
        InputFile inputFile5 = createInputFileFromPath("modules/" +
                "functionPathInjection/functionPathInjection.bal");
        context.fileSystem().add(inputFile5);
        InputFile inputFile6 = createInputFileFromPath("modules/" +
                "insecureEndpoint/insecureEndpoint.bal");
        context.fileSystem().add(inputFile6);
        InputFile inputFile7 = createInputFileFromPath("modules/" +
                "panicChecker/panicChecker.bal");
        context.fileSystem().add(inputFile7);
        InputFile inputFile8 = createInputFileFromPath("modules/" +
                "tooManyParameters/tooManyParameters.bal");
        context.fileSystem().add(inputFile8);

        // Setting up dummy rules
        CheckFactory checkFactory = checkFactory("B107", "B108");
        BallerinaSensor sensor = sensor(checkFactory);

        sensor.execute(context);

        // Assert if a results file was generated during the process
        Assertions.assertThat(Files.exists(Paths.get(context.fileSystem()
                .baseDir().getPath()).resolve(ISSUES_FILE_PATH))).isEqualTo(true);
    }

    private BallerinaSensor sensor(CheckFactory checkFactory) {

        return new BallerinaSensor(checkFactory, fileLinesContextFactory, new DefaultNoSonarFilter(), language());
    }
}
