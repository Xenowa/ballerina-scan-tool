package io.ballerina.scan;

import java.util.ArrayList;

public class InternalScannerContext {

    private final InternalReporter reporter;
    
    InternalScannerContext(ArrayList<Issue> issues) {

        this.reporter = new InternalReporter(issues);
    }

    InternalReporter getReporter() {

        return reporter;
    }
}
