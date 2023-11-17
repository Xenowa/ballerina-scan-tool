package org.wso2.ballerina.internal.miscellaneous.sonarqubeold;

import org.sonarsource.scanner.api.EmbeddedScanner;
import org.sonarsource.scanner.api.LogOutput;

import java.util.Map;
import java.util.Properties;

class ScannerFactory {

    private final Logs logger;

    public ScannerFactory(Logs logger) {
        this.logger = logger;
    }

    EmbeddedScanner create(Properties props, String isInvokedFrom) {
        String appName = "BalScannerCLI";
        String appVersion = "1.0";
        if (isInvokedFrom.contains("/")) {
            appName = isInvokedFrom.split("/")[0];
            appVersion = isInvokedFrom.split("/")[1];
        }

        return EmbeddedScanner.create(appName, appVersion, new DefaultLogOutput())
                .addGlobalProperties((Map) props);
    }

    class DefaultLogOutput implements LogOutput {
        @Override
        public void log(String formattedMessage, Level level) {

            switch (level) {
                case TRACE, DEBUG -> logger.debug(formattedMessage);
                case ERROR -> logger.error(formattedMessage);
                case WARN -> logger.warn(formattedMessage);
                default -> logger.info(formattedMessage);
            }
        }
    }
}
