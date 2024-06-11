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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractSensorTest {

    @TempDir
    public Path temp;
    protected Path baseDir;
    protected SensorContextTester context;

    @BeforeEach
    public void setup() {
        // Pointing the testing directory
        baseDir = Path.of(System.getProperty("user.dir") + "/"
                + "src/test/java/io/ballerina/sonar/sonar_bal_testing");

        // Setting the context for the testing directory
        context = SensorContextTester.create(baseDir);
    }

    protected InputFile createInputFileFromPath(String relativePath) {
        Path balFilePath = Path.of(baseDir.toString() + "/" + relativePath);

        String fileContent;
        try {
            fileContent = Files.readString(balFilePath).trim();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return TestInputFileBuilder.create("moduleKey", relativePath)
                .setModuleBaseDir(baseDir)
                .setType(InputFile.Type.MAIN)
                .setLanguage(language().getKey())
                .setCharset(StandardCharsets.UTF_8)
                .setContents(fileContent)
                .setStatus(InputFile.Status.SAME)
                .build();
    }

    protected BallerinaLanguage language() {
        return new BallerinaLanguage(new MapSettings().asConfig());
    }
}
