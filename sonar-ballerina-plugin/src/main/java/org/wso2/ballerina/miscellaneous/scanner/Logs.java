package org.wso2.ballerina.miscellaneous.scanner;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Logs {
    private DateTimeFormatter timeFormatter;
    private boolean debugEnabled = false;
    private PrintStream stdOut;
    private PrintStream stdErr;

    public Logs(PrintStream stdOut, PrintStream stdErr) {
        this.stdErr = stdErr;
        this.stdOut = stdOut;
        this.timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void debug(String message) {
        if (isDebugEnabled()) {
            LocalTime currentTime = LocalTime.now();
            String timestamp = currentTime.format(timeFormatter);
            stdOut.println(timestamp + " DEBUG: " + message);
        }
    }

    public void info(String message) {
        print(stdOut, "INFO: " + message);
    }

    public void warn(String message) {
        print(stdOut, "WARN: " + message);
    }

    public void error(String message) {
        print(stdErr, "ERROR: " + message);
    }

    public void error(String message, Throwable t) {
        print(stdErr, "ERROR: " + message);
        t.printStackTrace(stdErr);
    }

    private void print(PrintStream stream, String msg) {
        if (debugEnabled) {
            LocalTime currentTime = LocalTime.now();
            String timestamp = currentTime.format(timeFormatter);
            stream.println(timestamp + " " + msg);
        } else {
            stream.println(msg);
        }
    }
}
