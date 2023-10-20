package org.wso2.ballerina.plugin;

import org.sonar.api.config.Configuration;
import org.sonar.api.resources.AbstractLanguage;

public class BallerinaLanguage extends AbstractLanguage {
    private final Configuration configuration;

    public BallerinaLanguage(Configuration configuration) {
        super(BallerinaPlugin.BALLERINA_LANGUAGE_KEY, BallerinaPlugin.BALLERINA_LANGUAGE_NAME);
        this.configuration = configuration;
    }

    @Override
    public String[] getFileSuffixes() {
        String[] suffixes = configuration.getStringArray(BallerinaPlugin.BALLERINA_FILE_SUFFIXES_KEY);
        if (suffixes == null || suffixes.length == 0) {
            return BallerinaPlugin.BALLERINA_FILE_SUFFIXES_DEFAULT_VALUE.split(",");
        } else {
            return suffixes;
        }
    }
}