package org.wso2.ballerina.utilities;

public class Rule {

    private final String ruleID;
    private final String ruleDescription;
    private boolean ruleActivated;

    public Rule(String ruleID, String ruleDescription, boolean ruleActivated) {

        this.ruleID = ruleID;
        this.ruleDescription = ruleDescription;
        this.ruleActivated = ruleActivated;
    }

    public String getRuleID() {

        return ruleID;
    }

    public String getRuleDescription() {

        return ruleDescription;
    }

    public boolean ruleIsActivated() {

        return ruleActivated;
    }

    public void setRuleIsActivated(boolean ruleActivated) {

        this.ruleActivated = ruleActivated;
    }
}
