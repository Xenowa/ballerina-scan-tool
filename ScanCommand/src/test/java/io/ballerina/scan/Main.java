/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.ballerina.scan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ballerina.projects.Project;
import io.ballerina.projects.directory.ProjectLoader;
import io.ballerina.scan.utilities.ScanTomlFile;
import io.ballerina.scan.utilities.ScanUtils;

import java.io.PrintStream;
import java.nio.file.Path;

public class Main {

    private static final PrintStream outputStream = System.out;

    public static void main(String[] args) {
        Project project = ProjectLoader.loadProject(Path.of("ScanCommand" + System.getProperty("file.separator") +
                "bal-scan-tool-tester"));
        ScanTomlFile scanTomlFile = ScanUtils.retrieveScanTomlConfigurations(project);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(scanTomlFile, ScanTomlFile.class);
        outputStream.println(jsonOutput);
    }
}
