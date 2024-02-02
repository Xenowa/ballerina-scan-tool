package org.wso2.ballerina.internal.utilities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ScanTomlFile {
    private ArrayList<Platform> platforms = new ArrayList<>();
    private Set<Plugin> plugins = new HashSet<>();

    private Set<RuleToFilter> rules = new HashSet<>();

    public void setPlatform(Platform platform) {
        platforms.add(platform);
    }

    public ArrayList<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlugin(Plugin plugin) {
        plugins.add(plugin);
    }

    public Set<Plugin> getPlugins() {
        return plugins;
    }

    public void setRuleToFilter(RuleToFilter rule) {
        rules.add(rule);
    }

    public Set<RuleToFilter> getRulesToFilter() {
        return rules;
    }

    public static class Platform {
        private String name;
        private String path;
        private Map<String, Object> arguments;

        public Platform(String name, String path, Map<String, Object> arguments) {
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

    public static class Plugin {
        private String org;
        private String name;
        private String version;
        private String repository;

        public Plugin(String org, String name, String version, String repository) {
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

        public RuleToFilter(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
