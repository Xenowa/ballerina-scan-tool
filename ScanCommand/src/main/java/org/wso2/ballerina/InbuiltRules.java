package org.wso2.ballerina;

import java.util.HashMap;
import java.util.Map;

public class InbuiltRules {
    // Initializing the inbuilt rules
    public static final Map<String, Boolean> INBUILT_RULES = new HashMap<>();

    static {
        INBUILT_RULES.put("S107", true);
        INBUILT_RULES.put("S108", true);
    }

    // Disable all except user defined rule
    public static boolean activateUserDefinedRule(String userDefinedRule) {
        if (INBUILT_RULES.containsKey(userDefinedRule)) {
            INBUILT_RULES.replaceAll((ruleID, activatedStatus) -> ruleID.equals(userDefinedRule));
            return true;
        } else {
            return false;
        }
    }
}
