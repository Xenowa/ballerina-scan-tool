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
