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

package org.arc.scanner;

import io.ballerina.projects.plugins.CompilerPluginContext;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.internal.StaticCodeAnalyzerPlugin;

public class CustomStaticCodeAnalyzer extends StaticCodeAnalyzerPlugin {

    @Override
    public void init(CompilerPluginContext compilerPluginContext) {
        ScannerContext scannerContext = (ScannerContext) compilerPluginContext.userData().get("ScannerContext");
        compilerPluginContext.addCodeAnalyzer(new CustomCodeAnalyzer(scannerContext));
    }
}
