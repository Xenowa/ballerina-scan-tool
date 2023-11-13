package org.wso2.ballerina.platforms;

import java.io.PrintStream;

public class SemGrep extends Platform {
    @Override
    public void scan(String userFile, PrintStream outputStream) {
    }

    @Override
    public void scan(PrintStream outputStream) {
        outputStream.println("Platform support is not available yet!");
    }

    @Override
    public void handleParseIssue(String userFile) {
    }
}
