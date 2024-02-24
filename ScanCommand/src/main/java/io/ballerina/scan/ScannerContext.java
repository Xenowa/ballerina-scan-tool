package io.ballerina.scan;

import io.ballerina.projects.Document;
import io.ballerina.projects.Module;
import io.ballerina.projects.Project;

import java.util.ArrayList;

public class ScannerContext {

    private final Reporter reporter;
    private Document currentDocument = null;
    private Module currentModule = null;
    private Project currentProject = null;

    // Internal constructor and methods
    ScannerContext(ArrayList<Issue> issues,
                   Document currentDocument,
                   Module currentModule,
                   Project currentProject) {

        this.reporter = new Reporter(issues);
        this.currentDocument = currentDocument;
        this.currentModule = currentModule;
        this.currentProject = currentProject;
    }

    // External constructor and methods
    public ScannerContext(ArrayList<Issue> issues) {

        this.reporter = new Reporter(issues);
    }

    Document getCurrentDocument() {

        return currentDocument;
    }

    Module getCurrentModule() {

        return currentModule;
    }

    Project getCurrentProject() {

        return currentProject;
    }

    public Reporter getReporter() {

        return reporter;
    }
}
