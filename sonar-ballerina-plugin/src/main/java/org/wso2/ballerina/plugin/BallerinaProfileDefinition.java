package org.wso2.ballerina.plugin;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class BallerinaProfileDefinition implements BuiltInQualityProfilesDefinition {
    public static final String PATH_TO_JSON = "org/sonar/l10n/ballerina/rules/ballerina/Sonar_way_profile.json";
    
    @Override
    public void define(Context context) {
        // Create a new quality profile for Ballerina
        NewBuiltInQualityProfile ballerinaQualityProfile = context.createBuiltInQualityProfile(BallerinaPlugin.PROFILE_NAME, BallerinaPlugin.BALLERINA_LANGUAGE_KEY);

        // Load the new quality profile
        BuiltInQualityProfileJsonLoader.load(ballerinaQualityProfile
                , BallerinaPlugin.BALLERINA_REPOSITORY_KEY
                , PATH_TO_JSON
        );

        // Signal that the new profile has been loaded to SonarQube
        ballerinaQualityProfile.done();
    }
}