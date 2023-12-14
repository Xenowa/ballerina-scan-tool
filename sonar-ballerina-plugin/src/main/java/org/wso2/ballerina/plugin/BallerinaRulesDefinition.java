package org.wso2.ballerina.plugin;

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

public class BallerinaRulesDefinition implements RulesDefinition {

    private static final String RESOURCE_FOLDER = "org/sonar/l10n/ballerina/rules/ballerina";
    private final SonarRuntime runtime;

    public BallerinaRulesDefinition(SonarRuntime runtime) {
        this.runtime = runtime;
    }

    // To define rules we have 1 of the two ways below
    // 1. Add rules by the annotated class (Requires defining the visitors in a checks folder)
    // 2. Add rules by the rule keys
    @Override
    public void define(Context context) {
        NewRepository repository = context.createRepository(BallerinaPlugin.BALLERINA_REPOSITORY_KEY, BallerinaPlugin.BALLERINA_LANGUAGE_KEY);
        repository.setName(BallerinaPlugin.REPOSITORY_NAME);

        RuleMetadataLoader ruleMetadataLoader = new RuleMetadataLoader(RESOURCE_FOLDER, BallerinaProfileDefinition.PATH_TO_JSON, runtime);

        ruleMetadataLoader.addRulesByRuleKey(repository, BallerinaChecks.DEFAULT_CHECKS);
        repository.done();
    }
}