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

import static io.ballerina.sonar.Constants.CATEGORY;
import static io.ballerina.sonar.Constants.FILE_SUFFIXES_DEFAULT_VALUE;
import static io.ballerina.sonar.Constants.FILE_SUFFIXES_KEY;
import static io.ballerina.sonar.Constants.SUB_CATEGORY;

public class BallerinaPlugin implements Plugin {

    /**
     * <p> Represents the entry point for introducing SonarQube plugins. </p>
     *
     * <p>
     * This class should be defined in the MANIFEST file for SonarQube to engage the plugin. This method is engaged only
     * after the SonarQube:
     * </p>
     * <ul>
     *     <li>Web server starts</li>
     *     <li>Compute engine starts</li>
     *     <li>Sonar Scanner starts</li>
     * </ul>
     *
     * @param context The sonar plugin context that accepts configurations
     */
    @Override
    public void define(Context context) {
        context.addExtensions(
                BallerinaLanguage.class,
                BallerinaSensor.class,
                BallerinaRulesDefinition.class,
                BallerinaProfileDefinition.class,

                // The following defines a UI based customizable property which is available in the sonarqube
                // administration dashboard here: http://localhost:9000/admin/settings?category=ballerina
                PropertyDefinition.builder(FILE_SUFFIXES_KEY)
                        .defaultValue(FILE_SUFFIXES_DEFAULT_VALUE)
                        .name("File Suffixes")
                        .description("List of suffixes for files to analyze.")
                        .subCategory(SUB_CATEGORY)
                        .category(CATEGORY)
                        .multiValues(true)
                        .onQualifiers(Qualifiers.PROJECT)
                        .build()
        );
    }
}
