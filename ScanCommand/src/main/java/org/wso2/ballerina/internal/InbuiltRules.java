package org.wso2.ballerina.internal;

import org.wso2.ballerina.internal.utilities.Rule;

import java.util.HashMap;
import java.util.Map;

public class InbuiltRules {
    // Initializing the inbuilt rules
    // Map for inbuilt rules
    public static final HashMap<String, Rule> INBUILT_RULES = new HashMap<>();

    /**
     * Custom rules map is populated through
     * {@link org.wso2.ballerina.Reporter#generateCustomRuleID(String customRuleMessage)}
     */
    // Map for custom rules
    public static final Map<String, Rule> CUSTOM_RULES = new HashMap<>();

    // Populating inbuilt rules
    static {
        INBUILT_RULES.put("S107", new Rule("S107",
                "Functions should not have too many parameters",
                true));
        INBUILT_RULES.put("S108", new Rule("S108",
                "use check keyword instead of checkpanic",
                true));
    }
}
