package org.wso2.ballerina.scanner;

import java.util.Properties;

class Cli {

    private boolean debugEnabled = false;
    private boolean displayVersionOnly = false;
    private final Properties props = new Properties();
    private final Logs logger;

    public Cli(Logs logger) {
        this.logger = logger;
    }

    Properties properties() {
        return props;
    }

    Cli parse() {
        reset();
        props.putAll(System.getProperties());
        return this;
    }

    private void reset() {
        props.clear();
        debugEnabled = false;
        displayVersionOnly = false;
    }
}
