/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.sonar;

public class Constants {

    private Constants() {
    }

    // Language definition constants
    static final String LANGUAGE_KEY = "ballerina";
    static final String LANGUAGE_NAME = "Ballerina";

    // Profile and rule definition constants
    static final String PROFILE_NAME = "Ballerina way";
    static final String JSON_PROFILE_PATH = "io/ballerina/sonar/Ballerina_way_profile.json";
    static final String RULE_REPOSITORY_KEY = "ballerina";
    static final String RULE_REPOSITORY_NAME = "BallerinaAnalyzer";
    static final String RULE_RESOURCE_FOLDER = "io/ballerina/sonar";

    // Property definition constants
    static final String FILE_SUFFIXES_KEY = "sonar.ballerina.file.suffixes";
    static final String FILE_SUFFIXES_DEFAULT_VALUE = ".bal";
    static final String SUB_CATEGORY = "General";
    static final String CATEGORY = "Ballerina";
}
