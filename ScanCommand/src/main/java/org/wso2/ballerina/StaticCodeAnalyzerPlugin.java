package org.wso2.ballerina;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class StaticCodeAnalyzerPlugin extends CompilerPlugin {

    private static final String serializeContextFile = "serialized-context-out.json";
    private static final Gson gson = new Gson();
    private static final Type listOfIssuesType = new TypeToken<ArrayList<Issue>>() {
    }.getType();
    private final Map<CompilerPluginContext, ScannerContext> scannerContexts = new HashMap<>();

    // Retrieve the deserialized context from file
    static ArrayList<Issue> getIssues() {

        Path serializedContextFilePath = Path.of(serializeContextFile);
        if (Files.exists(serializedContextFilePath)) {
            try {
                Reader fileReader = new FileReader(serializeContextFile, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(fileReader);
                ArrayList<Issue> externalIssues = gson.fromJson(reader, listOfIssuesType);
                reader.close();

                // delete the file after getting the context
                Files.delete(serializedContextFilePath);

                return externalIssues;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    public ScannerContext getScannerContext(CompilerPluginContext compilerPluginContext) {

        if (scannerContexts.containsKey(compilerPluginContext)) {
            return scannerContexts.get(compilerPluginContext);
        }

        // Create new context when requested by compiler plugins
        ArrayList<Issue> externalIssues = new ArrayList<>();
        ScannerContext scannerContext = new ScannerContext(externalIssues);
        scannerContexts.put(compilerPluginContext, scannerContext);
        return scannerContext;
    }

    // Save the serialized context to file
    public synchronized void complete(CompilerPluginContext compilerPluginContext) {

        ScannerContext scannerContext = getScannerContext(compilerPluginContext);

        try {
            Writer fileWriter = new FileWriter(serializeContextFile, StandardCharsets.UTF_8);
            JsonWriter writer = new JsonWriter(fileWriter);
            if (!Files.exists(Path.of(serializeContextFile))) {
                // Check if file already exists if not create one and save the Analysis issues directly
                gson.toJson(scannerContext.getReporter().getIssues(), listOfIssuesType, writer);
            } else {
                // Read the context from file
                Reader fileReader = new FileReader(serializeContextFile, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(fileReader);
                ArrayList<Issue> deserializedExternalIssues = gson.fromJson(reader, listOfIssuesType);
                reader.close();

                if (deserializedExternalIssues == null) {
                    gson.toJson(scannerContext.getReporter().getIssues(), listOfIssuesType, writer);
                } else {
                    // Save the issues from in memory context to the file
                    deserializedExternalIssues.addAll(scannerContext.getReporter().getIssues());
                    gson.toJson(deserializedExternalIssues, listOfIssuesType, writer);
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
