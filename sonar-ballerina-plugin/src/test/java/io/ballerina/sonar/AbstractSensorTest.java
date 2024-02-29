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
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.rule.CheckFactory;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.rule.internal.NewActiveRule;
import org.sonar.api.batch.sensor.internal.SensorContextTester;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.measures.FileLinesContext;
import org.sonar.api.measures.FileLinesContextFactory;
import org.sonar.api.rule.RuleKey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class AbstractSensorTest {

    @TempDir
    public Path temp;
    protected Path baseDir;
    protected SensorContextTester context;
    protected FileLinesContextFactory fileLinesContextFactory = Mockito.mock(FileLinesContextFactory.class);

    @BeforeEach
    public void setup() throws IOException {
        // Pointing the testing directory
        baseDir = Path.of(System.getProperty("user.dir")
                + "/"
                + "src/test/java/io/ballerina/sonar/sonar_bal_testing");

        // Setting the context for the testing directory
        context = SensorContextTester.create(baseDir);

        FileLinesContext fileLinesContext = Mockito.mock(FileLinesContext.class);
        Mockito.when(
                fileLinesContextFactory.createFor(
                        ArgumentMatchers.any(
                                InputFile.class
                        )
                )
        ).thenReturn(fileLinesContext);
    }

    protected CheckFactory checkFactory(String... ruleKeys) {

        ActiveRulesBuilder builder = new ActiveRulesBuilder();
        for (String ruleKey : ruleKeys) {
            NewActiveRule newRule = new NewActiveRule.Builder()
                    .setRuleKey(RuleKey.of(BallerinaPlugin.BALLERINA_REPOSITORY_KEY, ruleKey))
                    .setName(ruleKey)
                    .build();
            builder.addRule(newRule);
        }
        context.setActiveRules(builder.build());
        return new CheckFactory(context.activeRules());
    }

    protected InputFile createInputFile(String relativePath, String content, InputFile.Status status) {

        return TestInputFileBuilder.create("moduleKey", relativePath)
                .setModuleBaseDir(baseDir)
                .setType(InputFile.Type.MAIN)
                .setLanguage(language().getKey())
                .setCharset(StandardCharsets.UTF_8)
                .setContents(content)
                .setStatus(status)
                .build();
    }

    protected InputFile createInputFileFromPath(String relativePath) {

        Path balFilePath = Path.of(baseDir.toString() + "/" + relativePath);

        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(balFilePath), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
