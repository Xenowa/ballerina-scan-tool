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

package io.ballerina.scan.utilities;

import io.ballerina.scan.Rule;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RuleMap implements Map<String, Rule> {

    Map<String, Rule> rulesMap = new HashMap<>();

    @Override
    public int size() {
        return rulesMap.size();
    }

    @Override
    public boolean isEmpty() {
        return rulesMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return rulesMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return rulesMap.containsValue(value);
    }

    @Override
    public Rule get(Object key) {
        return rulesMap.get(key);
    }

    @Override
    public Rule put(String key, Rule value) {
        // Check if rule ID is equal to the ID of the value and only put if that's the case
        if (key.equals(value.getRuleID())) {
            rulesMap.put(key, value);
            return value;
        } else {
            throw new IllegalArgumentException("Key '" + key +
                    "' Does not match ruleID '" + value.getRuleID() + "' of provided rule!");
        }
    }

    @Override
    public Rule remove(Object key) {
        return rulesMap.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Rule> m) {
        rulesMap.putAll(m);
    }

    @Override
    public void clear() {
        rulesMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return rulesMap.keySet();
    }

    @Override
    public Collection<Rule> values() {
        return rulesMap.values();
    }

    @Override
    public Set<Entry<String, Rule>> entrySet() {
        return rulesMap.entrySet();
    }

    public RuleMap copy() {
        RuleMap clonedMap = new RuleMap();
        clonedMap.putAll(rulesMap);

        return clonedMap;
    }
}
