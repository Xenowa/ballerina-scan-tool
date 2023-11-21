package org.wso2.ballerina.miscellaneous;

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.wso2.ballerina.plugin.BallerinaPlugin;

public class BallerinaProfileDefinitionTest implements BuiltInQualityProfilesDefinition {
    // Custom rules
    @Override
    public void define(Context context) {
        // Create a new quality profile for Ballerina
        NewBuiltInQualityProfile ballerinaQualityProfile = context.createBuiltInQualityProfile(BallerinaPlugin.PROFILE_NAME, BallerinaPlugin.BALLERINA_LANGUAGE_KEY);

        // =====================
        // Adding External Rules
        // =====================
        // it's possible to overrid the load method provided by the BuiltInQualityProfileLoader and
        // Pass the custom rule strings when the plugin is being initiated
        // user can run "bal scan rules"
        // it sends back the rules, these can then be filtered out and there rule ID's can be obtained
        // those can be passed as the following:
        // example let's pass S109 which is loaded from a custom bal scan plugin
        ballerinaQualityProfile.activateRule(BallerinaPlugin.BALLERINA_REPOSITORY_KEY, "S107");
        ballerinaQualityProfile.activateRule(BallerinaPlugin.BALLERINA_REPOSITORY_KEY, "S108");
        ballerinaQualityProfile.activateRule(BallerinaPlugin.BALLERINA_REPOSITORY_KEY, "S109");

        // Signal that the new profile has been loaded to SonarQube
        ballerinaQualityProfile.done();
    }
}