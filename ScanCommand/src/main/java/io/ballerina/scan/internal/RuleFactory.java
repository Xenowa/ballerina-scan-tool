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

package io.ballerina.scan.internal;

import io.ballerina.scan.Rule;
import io.ballerina.scan.Severity;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static io.ballerina.scan.internal.ScanToolConstants.BALLERINA_RULE_PREFIX;
import static io.ballerina.scan.internal.ScanToolConstants.PATH_SEPARATOR;

public class RuleFactory {

    private RuleFactory() {
    }

    static Rule createRule(int numericId, String description, Severity severity) {
        return new RuleIml(BALLERINA_RULE_PREFIX + numericId, numericId, description, severity);
    }

    public static Rule createRule(int numericId, Severity severity, String description) {
        // Getting org/name from URL through protection domain
        URL jarUrl = severity.getClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();
        URI jarUri;
        try {
            jarUri = jarUrl.toURI();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }

        Path jarPath = Paths.get(jarUri);
        String pluginName = Optional.of(jarPath)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse("");

        String pluginOrg = Optional.of(jarPath)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getParent)
                .map(Path::getFileName)
                .map(Path::toString)
                .orElse("");

        String fullyQualifiedId = String.format("%s" + PATH_SEPARATOR + "%s:%s%d", pluginOrg,
                pluginName, BALLERINA_RULE_PREFIX, numericId);

        return new RuleIml(fullyQualifiedId, numericId, description, severity);
    }
}
