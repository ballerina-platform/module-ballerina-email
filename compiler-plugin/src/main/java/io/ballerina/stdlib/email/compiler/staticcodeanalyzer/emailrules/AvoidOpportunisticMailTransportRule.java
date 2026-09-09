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

package io.ballerina.stdlib.email.compiler.staticcodeanalyzer.emailrules;

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailClientConfigContext;

import java.util.Optional;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.namesEnumMember;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailRule.AVOID_OPPORTUNISTIC_MAIL_TRANSPORT;

/**
 * Rule to detect a mail client that falls back to plaintext when TLS is unavailable.
 * <p>
 * {@code START_TLS_AUTO} upgrades the connection when the server advertises STARTTLS and continues in plaintext when
 * it does not. Whether the credentials are encrypted is therefore decided by the server's greeting, which an
 * attacker positioned on the path can rewrite: stripping the advertisement is enough to have the client send them in
 * the clear, with nothing to indicate anything went wrong. {@code START_TLS_ALWAYS} fails instead of downgrading.
 */
public class AvoidOpportunisticMailTransportRule implements EmailClientConfigRule {

    private static final String SECURITY_FIELD = "security";
    private static final String START_TLS_AUTO = "START_TLS_AUTO";

    @Override
    public void analyze(EmailClientConfigContext context) {
        Optional<ExpressionNode> security = context.getConfigFieldValue(SECURITY_FIELD);
        if (security.isPresent() && namesEnumMember(security.get(), START_TLS_AUTO)) {
            context.reportConfigFieldIssue(SECURITY_FIELD, getRuleId());
        }
    }

    @Override
    public int getRuleId() {
        return AVOID_OPPORTUNISTIC_MAIL_TRANSPORT.getId();
    }

    @Override
    public boolean isApplicable(EmailClientConfigContext context) {
        return context.hasClientConfig();
    }
}
