package org.wso2.ballerina.plugin;

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class BallerinaPlugin implements Plugin {

    // Global constants
    public static final String BALLERINA_LANGUAGE_KEY = "ballerina";
    public static final String BALLERINA_LANGUAGE_NAME = "Ballerina";
    public static final String BALLERINA_REPOSITORY_KEY = "ballerina";
    public static final String REPOSITORY_NAME = "SonarAnalyzer";
    public static final String PROFILE_NAME = "Sonar way";
    public static final String BALLERINA_FILE_SUFFIXES_KEY = "sonar.ballerina.file.suffixes";
    public static final String BALLERINA_FILE_SUFFIXES_DEFAULT_VALUE = ".bal";
    public static final String PERFORMANCE_MEASURE_ACTIVATION_PROPERTY = "sonar.ballerina.performance.measure";
    public static final String PERFORMANCE_MEASURE_DESTINATION_FILE = "sonar.ballerina.performance.measure.json";

    // Required Categories
    private static final String GENERAL = "General";
    private static final String BALLERINA_CATEGORY = "Ballerina";

    @Override
    public void define(Context context) {

        context.addExtensions(
                BallerinaLanguage.class,
                BallerinaSensor.class,
                BallerinaRulesDefinition.class,
                BallerinaProfileDefinition.class,

                // The following defines a UI based customizable property which is available in the sonarqube
                // administration dashboard here: http://localhost:9000/admin/settings?category=ballerina
                PropertyDefinition.builder(BALLERINA_FILE_SUFFIXES_KEY)
                        .defaultValue(BALLERINA_FILE_SUFFIXES_DEFAULT_VALUE)
                        .name("File Suffixes")
                        .description("List of suffixes for files to analyze.")
                        .subCategory(GENERAL)
                        .category(BALLERINA_CATEGORY)
                        .multiValues(true)
                        .onQualifiers(Qualifiers.PROJECT)
                        .build()
        );
    }
}
