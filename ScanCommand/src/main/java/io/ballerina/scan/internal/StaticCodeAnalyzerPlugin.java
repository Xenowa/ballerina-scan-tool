/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.internal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.projects.plugins.CompilerPluginContext;
import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;

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
import java.util.List;

public abstract class StaticCodeAnalyzerPlugin extends CompilerPlugin {

    private static final String SERIALIZE_CONTEXT_FILE = "serialized-context-out.json";
    private static final Gson gson = new Gson();
    private static final Type listOfIssuesType = new TypeToken<ArrayList<IssueIml>>() {
    }.getType();
    private ScannerContextIml currentScannerContext = null;

    /**
     * Used for loading custom rules from compiler plugins to the scan tool.
     */
    // It's not just to display the rules. This is what we will rely on in the final report for description, kind,
    // severity, etc. too.
    public abstract List<Rule> rules();

    public synchronized ScannerContext getScannerContext(CompilerPluginContext compilerPluginContext) {
        // Implementations here will change once scanner context can be retrieved from compilerPluginContext
        if (currentScannerContext != null) {
            return currentScannerContext;
        }

        // TODO: To be created from the scan tool side ones project API fix is in effect
        List<Issue> externalIssues = new ArrayList<>();
        currentScannerContext = new ScannerContextIml(externalIssues);
        return currentScannerContext;
    }

    // TODO: To be removed ones project API fix is in effect
    public synchronized void complete() {
        if (currentScannerContext != null) {
            List<Issue> existingIssues = currentScannerContext.getAllIssues();

            if (!existingIssues.isEmpty()) {
                try {
                    if (Files.exists(Path.of(SERIALIZE_CONTEXT_FILE))) {
                        // Read the context from file
                        Reader fileReader = new FileReader(SERIALIZE_CONTEXT_FILE, StandardCharsets.UTF_8);
                        JsonReader reader = new JsonReader(fileReader);
                        List<Issue> deserializedExternalIssues = gson.fromJson(reader, listOfIssuesType);
                        reader.close();

                        Writer fileWriter = new FileWriter(SERIALIZE_CONTEXT_FILE, StandardCharsets.UTF_8);
                        JsonWriter writer = new JsonWriter(fileWriter);

                        if (deserializedExternalIssues == null) {
                            gson.toJson(existingIssues, listOfIssuesType, writer);
                        } else {
                            // Save the issues from in memory context to the file
                            deserializedExternalIssues.addAll(existingIssues);
                            gson.toJson(deserializedExternalIssues, listOfIssuesType, writer);
                        }
                        writer.close();
                    } else {
                        Writer fileWriter = new FileWriter(SERIALIZE_CONTEXT_FILE, StandardCharsets.UTF_8);
                        JsonWriter writer = new JsonWriter(fileWriter);
                        gson.toJson(existingIssues, listOfIssuesType, writer);
                        writer.close();
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    // TODO: To be removed ones project API fix is in effect
    // Retrieve the deserialized context from file
    static List<Issue> getIssues() {
        Path serializedContextFilePath = Path.of(SERIALIZE_CONTEXT_FILE);
        if (Files.exists(serializedContextFilePath)) {
            try {
                Reader fileReader = new FileReader(SERIALIZE_CONTEXT_FILE, StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(fileReader);
                List<Issue> externalIssues = gson.fromJson(reader, listOfIssuesType);
                reader.close();

                // delete the file after getting the context
                Files.delete(serializedContextFilePath);

                return externalIssues;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        return null;
    }
}
