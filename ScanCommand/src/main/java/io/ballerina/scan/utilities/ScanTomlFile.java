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

package io.ballerina.scan.utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScanTomlFile {

    private final List<Platform> platforms = new ArrayList<>();
    private final Set<Analyzer> analyzers = new HashSet<>();
    private final Set<RuleToFilter> rulesToInclude = new HashSet<>();
    private final Set<RuleToFilter> rulesToExclude = new HashSet<>();

    void setPlatform(Platform platform) {
        platforms.add(platform);
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    void setAnalyzer(Analyzer analyzer) {
        analyzers.add(analyzer);
    }

    public Set<Analyzer> getAnalyzers() {
        return analyzers;
    }

    void setRuleToInclude(RuleToFilter rule) {
        rulesToInclude.add(rule);
    }

    public Set<RuleToFilter> getRulesToInclude() {
        return rulesToInclude;
    }

    void setRuleToExclude(RuleToFilter rule) {
        rulesToExclude.add(rule);
    }

    public Set<RuleToFilter> getRulesToExclude() {
        return rulesToExclude;
    }

    public static class Platform {

        private String name;
        private String path;
        private Map<String, Object> arguments;

        Platform(String name, String path, Map<String, Object> arguments) {

            this.name = name;
            this.path = path;
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public Map<String, Object> getArguments() {
            return arguments;
        }
    }

    public static class Analyzer {

        private String org;
        private String name;
        private String version;
        private String repository;

        Analyzer(String org, String name, String version, String repository) {

            this.org = org;
            this.name = name;
            this.version = version;
            this.repository = repository;
        }

        public String getOrg() {
            return org;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getRepository() {
            return repository;
        }
    }

    public static class RuleToFilter {

        private String id;

        RuleToFilter(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
