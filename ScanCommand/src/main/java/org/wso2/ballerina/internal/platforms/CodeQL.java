
package org.wso2.ballerina.internal.platforms;

import java.io.PrintStream;

public class CodeQL extends Platform {
    @Override
    public void scan(String userFile, PrintStream outputStream) {
    }

    @Override
    public void scan(PrintStream outputStream) {
        outputStream.println("Platform support is not available yet!");
    }
}
