/*
 *  Copyright (c) 2024, WSO2 LLC. (https://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.scan.internal;

import io.ballerina.scan.Issue;
import io.ballerina.scan.Rule;
import io.ballerina.scan.Source;
import io.ballerina.tools.diagnostics.Location;
import org.wso2.ballerinalang.compiler.diagnostic.BLangDiagnosticLocation;

public class IssueIml implements Issue {

    private final BLangDiagnosticLocation location;
    private final RuleIml rule;
    private final Source source;
    // There can be more than one ballerina file which has the same name, so we store it in the following format:
    // fileName = "moduleName/main.bal"
    private final String fileName;
    private final String filePath;

    IssueIml(Location location,
             Rule rule,
             Source source,
             String fileName,
             String filePath) {
        this.location = new BLangDiagnosticLocation(location.lineRange().fileName(),
                location.lineRange().startLine().line(), location.lineRange().endLine().line(),
                location.lineRange().startLine().offset(), location.lineRange().endLine().offset(),
                location.textRange().startOffset(), location.textRange().length());
        this.rule = (RuleIml) rule;
        this.source = source;
        this.fileName = fileName;
        this.filePath = filePath;
    }

    @Override
    public Location location() {
        return location;
    }

    @Override
    public Rule rule() {
        return rule;
    }

    @Override
    public Source source() {
        return source;
    }

    public String filePath() {
        return filePath;
    }

    public String fileName() {
        return fileName;
    }
}
