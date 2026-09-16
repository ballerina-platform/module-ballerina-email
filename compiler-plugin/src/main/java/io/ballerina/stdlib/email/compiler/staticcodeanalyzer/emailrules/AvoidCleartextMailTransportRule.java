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
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailRule.AVOID_CLEARTEXT_MAIL_TRANSPORT;

/**
 * Rule to detect a mail client that never negotiates TLS.
 * <p>
 * {@code START_TLS_NEVER} keeps the connection in plaintext for its whole life. The mailbox credentials are sent on
 * that connection, so anyone on the network path reads them along with every message. The field defaults to
 * {@code SSL}, so plaintext is only ever reached by asking for it.
 */
public class AvoidCleartextMailTransportRule implements EmailClientConfigRule {

    private static final String SECURITY_FIELD = "security";
    private static final String START_TLS_NEVER = "START_TLS_NEVER";

    @Override
    public void analyze(EmailClientConfigContext context) {
        Optional<ExpressionNode> security = context.getConfigFieldValue(SECURITY_FIELD);
        if (security.isPresent() && namesEnumMember(security.get(), START_TLS_NEVER)) {
            context.reportConfigFieldIssue(SECURITY_FIELD, getRuleId());
        }
    }

    @Override
    public int getRuleId() {
        return AVOID_CLEARTEXT_MAIL_TRANSPORT.getId();
    }

    @Override
    public boolean isApplicable(EmailClientConfigContext context) {
        return context.hasClientConfig();
    }
}
