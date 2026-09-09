/*
 *  Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 *  OF ANY KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.stdlib.email.compiler.staticcodeanalyzer;

import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.emailrules.AvoidCleartextMailTransportRule;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.emailrules.AvoidOpportunisticMailTransportRule;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.emailrules.AvoidUnverifiedHostnamesRule;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.emailrules.AvoidWeakTlsProtocolsRule;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.emailrules.EmailClientConfigRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine to execute email client configuration rules.
 */
public class EmailClientConfigRulesEngine {

    private final List<EmailClientConfigRule> rules;

    public EmailClientConfigRulesEngine() {
        this.rules = new ArrayList<>();
        initializeDefaultRules();
    }

    public void executeRules(EmailClientConfigContext context) {
        for (EmailClientConfigRule rule : rules) {
            if (rule.isApplicable(context)) {
                rule.analyze(context);
            }
        }
    }

    public void addRule(EmailClientConfigRule rule) {
        if (rule != null && !rules.contains(rule)) {
            rules.add(rule);
        }
    }

    private void initializeDefaultRules() {
        addRule(new AvoidUnverifiedHostnamesRule());
        addRule(new AvoidCleartextMailTransportRule());
        addRule(new AvoidOpportunisticMailTransportRule());
        addRule(new AvoidWeakTlsProtocolsRule());
        // Add more default rules here as needed
    }
}
