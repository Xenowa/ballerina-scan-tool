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

import io.ballerina.scan.utilities.Rule;

import java.util.HashMap;

public class InbuiltRules {

    // Initializing the inbuilt rules
    // Map for inbuilt rules
    static final HashMap<String, Rule> INBUILT_RULES = new HashMap<>();

    /**
     * Custom rules map is populated through
     * {@link Reporter#generateCustomRuleID(String customRuleMessage)}.
     */
    // Map for custom rules
    static final HashMap<String, Rule> CUSTOM_RULES = new HashMap<>();

    // Populating inbuilt rules
    static {
        INBUILT_RULES.put("S107", new Rule("S107",
                "Functions should not have too many parameters!",
                true));
        INBUILT_RULES.put("S108", new Rule("S108",
                "Avoid checkpanic, prefer explicit error handling using check keyword instead!",
                true));
    }
}
