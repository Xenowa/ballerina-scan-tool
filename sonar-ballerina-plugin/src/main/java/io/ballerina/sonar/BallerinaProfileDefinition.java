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

import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;
import org.sonarsource.analyzer.commons.BuiltInQualityProfileJsonLoader;

public class BallerinaProfileDefinition implements BuiltInQualityProfilesDefinition {

    public static final String PATH_TO_JSON = "org/sonar/l10n/ballerina/rules/ballerina/Sonar_way_profile.json";

    @Override
    public void define(Context context) {
        // Create a new quality profile for Ballerina
        NewBuiltInQualityProfile ballerinaQualityProfile = context.createBuiltInQualityProfile(
                BallerinaPlugin.PROFILE_NAME,
                BallerinaPlugin.BALLERINA_LANGUAGE_KEY);

        // Load the new quality profile
        BuiltInQualityProfileJsonLoader.load(ballerinaQualityProfile
                , BallerinaPlugin.BALLERINA_REPOSITORY_KEY
                , PATH_TO_JSON
        );

        // Signal that the new profile has been loaded to SonarQube
        ballerinaQualityProfile.done();
    }
}
