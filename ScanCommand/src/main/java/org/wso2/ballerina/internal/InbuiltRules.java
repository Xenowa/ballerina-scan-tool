package org.wso2.ballerina.internal;

import java.util.HashMap;
import java.util.Map;

public class InbuiltRules {
    // Initializing the inbuilt rules
    // Map for inbuilt rules
    public static final Map<String, Boolean> INBUILT_RULES = new HashMap<>();

    /**
     * Custom rules map is populated through
     * {@link org.wso2.ballerina.internal.ReportLocalIssue#generateCustomRuleID(String customRuleMessage)}
     */
    // Map for custom rules
    public static final Map<String, String> CUSTOM_RULES = new HashMap<>();

    // Populating inbuilt rules
    static {
        INBUILT_RULES.put("S107", true);
        INBUILT_RULES.put("S108", true);
    }

    // Disable all except user defined rule
    // bal scan --rule=S107
    public static boolean activateUserDefinedRule(String userDefinedRule) {
        if (INBUILT_RULES.containsKey(userDefinedRule)) {
            INBUILT_RULES.replaceAll((ruleID, activatedStatus) -> ruleID.equals(userDefinedRule));
            return true;
        } else {
            return false;
        }
    }

    // Create a method to accept the user defined rules toml file, parse it and check against existing rules
    // and enable only those instead of above approach moving forwards
}
