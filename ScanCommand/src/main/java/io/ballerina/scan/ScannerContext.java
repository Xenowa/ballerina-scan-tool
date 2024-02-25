package io.ballerina.scan;

import java.util.ArrayList;

public class ScannerContext {

    private final Reporter reporter;

    // Internal constructor and methods
    ScannerContext(ArrayList<Issue> issues) {

        this.reporter = new Reporter(issues);
    }

    public synchronized Reporter getReporter() {

        return reporter;
    }
}
