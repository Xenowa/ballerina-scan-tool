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

import org.assertj.core.api.Assertions;
import org.sonar.api.batch.fs.InputFile;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

public class BallerinaSensorTest extends AbstractSensorTest {

    private static final String ISSUES_FILE_PATH = "ballerina-analysis-results.json";

    @Test(description = "Test the BallerinaSensor")
    void test_ballerina_sensor() {
        InputFile ballerinaFile = createInputFileFromPath("main.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" +
                "credentialsNotHardCoded/credentialsNotHardCoded.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" + "databaseInjection/databaseInjection.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" + "emptyFunction/emptyFunction.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" +
                "functionPathInjection/functionPathInjection.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" + "insecureEndpoint/insecureEndpoint.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" + "panicChecker/panicChecker.bal");
        context.fileSystem().add(ballerinaFile);
        ballerinaFile = createInputFileFromPath("modules/" + "tooManyParameters/tooManyParameters.bal");
        context.fileSystem().add(ballerinaFile);

        BallerinaSensor sensor = sensor();
        sensor.execute(context);
        Assertions.assertThat(Files.exists(Paths.get(context.fileSystem().baseDir().getPath())
                        .resolve(ISSUES_FILE_PATH)))
                .isEqualTo(true);
    }

    private BallerinaSensor sensor() {
        return new BallerinaSensor(language());
    }
}
