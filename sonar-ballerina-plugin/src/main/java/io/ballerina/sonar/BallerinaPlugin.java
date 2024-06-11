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

import org.sonar.api.Plugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

public class BallerinaPlugin implements Plugin {

    // Global constants
    public static final String BALLERINA_LANGUAGE_KEY = "ballerina";
    public static final String BALLERINA_LANGUAGE_NAME = "Ballerina";
    public static final String BALLERINA_REPOSITORY_KEY = "ballerina";
    public static final String REPOSITORY_NAME = "BallerinaAnalyzer";
    public static final String PROFILE_NAME = "Ballerina way";
    public static final String BALLERINA_FILE_SUFFIXES_KEY = "sonar.ballerina.file.suffixes";
    public static final String BALLERINA_FILE_SUFFIXES_DEFAULT_VALUE = ".bal";

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
