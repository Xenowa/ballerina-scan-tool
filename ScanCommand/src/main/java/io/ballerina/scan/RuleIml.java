/*
 *
 *  * Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *  *
 *  * WSO2 LLC. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied. See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package io.ballerina.scan;

public class RuleIml implements Rule {

    private final String id;
    private final int numericId;
    private final String description;
    private final Severity severity;

    // TODO: Identify a way to generate the rule from a rule Factory as opposed to directly allowing plugin developer
//  Create the rule
    public RuleIml(int numericId, String description, Severity severity) {
        this.id = Integer.toString(numericId);
        this.numericId = numericId;
        this.description = description;
        this.severity = severity;
    }

    public String getId() {
        return id;
    }

    public int getNumericId() {
        return numericId;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }
}
