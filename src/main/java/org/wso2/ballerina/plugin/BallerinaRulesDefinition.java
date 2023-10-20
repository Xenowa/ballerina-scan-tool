package org.wso2.ballerina.plugin;

import org.sonar.api.SonarRuntime;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonarsource.analyzer.commons.RuleMetadataLoader;

import java.util.ArrayList;
import java.util.List;

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

        // Setting up adding the rules by keys instead of classes for testing purposes
        // NOTE: Rule number 001 is not valid
        List<String> BALLERINA_CHECKS = new ArrayList<>();
        BALLERINA_CHECKS.add("S107");
        ruleMetadataLoader.addRulesByRuleKey(repository, BALLERINA_CHECKS);

        // the meta data loading from the classes have been commented out for testing purposes
        // ruleMetadataLoader.addRulesByAnnotatedClass(repository, BALLERINA_CHECKS);
        repository.done();
    }
}