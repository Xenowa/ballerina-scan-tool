package org.wso2.ballerina.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.sonar.api.utils.log.LogTesterJUnit5;

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

    @RegisterExtension
    public LogTesterJUnit5 logTester = new LogTesterJUnit5();

    @BeforeEach
    public void setup() throws IOException {
        baseDir = Files.createTempDirectory(temp.toFile().toPath(), null);
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

    protected BallerinaLanguage language() {
        return new BallerinaLanguage(new MapSettings().asConfig());
    }
}