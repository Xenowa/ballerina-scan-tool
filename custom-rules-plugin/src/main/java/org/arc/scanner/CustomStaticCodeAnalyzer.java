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
import io.ballerina.scan.Rule;
import io.ballerina.scan.ScannerContext;
import io.ballerina.scan.Severity;
import io.ballerina.scan.StaticCodeAnalyzerPlugin;
import io.ballerina.scan.utilities.RuleMap;

public class CustomStaticCodeAnalyzer extends StaticCodeAnalyzerPlugin {

    private final RuleMap customRules;

    public CustomStaticCodeAnalyzer() {
        this.customRules = new RuleMap();

        customRules.put("S109", new Rule("S109", "Add a nested comment explaining why" +
                " this function is empty or complete the implementation.", Severity.CODE_SMELL, true));
        customRules.put("S110", new Rule("S110", "rule 110",
                Severity.CODE_SMELL, true));
        customRules.put("S111", new Rule("S111", "rule 111",
                Severity.CODE_SMELL, true));
        customRules.put("S112", new Rule("S112", "rule 112",
                Severity.CODE_SMELL, true));
    }

    @Override
    public RuleMap rules() {
        return customRules.copy();
    }

    @Override
    public void init(CompilerPluginContext compilerPluginContext) {
        ScannerContext scannerContext = getScannerContext(compilerPluginContext);

        compilerPluginContext.addCodeAnalyzer(new CustomCodeAnalyzer(this, scannerContext,
                customRules));
    }
}
