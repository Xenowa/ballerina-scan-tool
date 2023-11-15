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
