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

import io.ballerina.scan.Rule;
import io.ballerina.scan.Severity;

import java.util.HashMap;
import java.util.Map;

public class InbuiltRules {

    static final Map<Integer, Rule> INBUILT_RULES = new HashMap<>();

    static {
        generateAndAddRule(107,
                "Functions should not have too many parameters!",
                Severity.CODE_SMELL);
        generateAndAddRule(108,
                "Avoid checkpanic, prefer explicit error handling using check keyword instead!",
                Severity.CODE_SMELL);
    }

    private static void generateAndAddRule(int id, String description, Severity severity) {
        Rule newRule = RuleFactory.createRule(id, description, severity);
        INBUILT_RULES.put(newRule.numericId(), newRule);
    }

    private InbuiltRules() {
    }
}
