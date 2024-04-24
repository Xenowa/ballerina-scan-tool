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

import io.ballerina.projects.plugins.CompilerPlugin;
import io.ballerina.scan.Rule;

import java.util.List;

public abstract class StaticCodeAnalyzerPlugin extends CompilerPlugin {

    /**
     * Used for loading custom rules from compiler plugins to the scan tool.
     */
    // It's not just to display the rules. This is what we will rely on in the final report for description, kind,
    // severity, etc. too.
    public abstract List<Rule> rules();
}
