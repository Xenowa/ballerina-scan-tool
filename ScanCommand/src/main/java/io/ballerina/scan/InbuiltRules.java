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

import java.util.ArrayList;
import java.util.List;

public class InbuiltRules {

    // Initializing the inbuilt rules
    // Map for inbuilt rules
    static final List<Rule> INBUILT_RULES = new ArrayList<>();
    private static final String REPORTED_SOURCE = "scan_tool";

    // Populating inbuilt rules
    static {
        INBUILT_RULES.add(RuleFactory.createRule(107,
                "Functions should not have too many parameters!",
                Severity.CODE_SMELL, REPORTED_SOURCE));
        INBUILT_RULES.add(RuleFactory.createRule(108,
                "Avoid checkpanic, prefer explicit error handling using check keyword instead!",
                Severity.CODE_SMELL, REPORTED_SOURCE));
    }
}
