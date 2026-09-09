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
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailClientConfigContext;

import java.util.Optional;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.findField;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getEffectiveExpression;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailRule.AVOID_UNVERIFIED_SERVER_HOSTNAMES;

/**
 * Rule to detect a mail client that does not verify the server's hostname during TLS.
 * <p>
 * Disabling hostname verification accepts a certificate for any hostname as proof of the server's identity, which
 * defeats what TLS is meant to guarantee: an attacker able to intercept the connection can present a certificate
 * for a different, but still valid, hostname and go unnoticed.
 */
public class AvoidUnverifiedHostnamesRule implements EmailClientConfigRule {

    private static final String SECURE_SOCKET = "secureSocket";
    private static final String VERIFY_HOST_NAME = "verifyHostName";
    private static final String FALSE_LITERAL = "false";

    @Override
    public void analyze(EmailClientConfigContext context) {
        Optional<SpecificFieldNode> verifyHostName = context.getNestedConfigRecord(SECURE_SOCKET)
                .flatMap(secureSocket -> findField(secureSocket, VERIFY_HOST_NAME));
        if (verifyHostName.isEmpty()) {
            return;
        }
        Optional<ExpressionNode> value = verifyHostName.get().valueExpr();
        if (value.isPresent()
                && FALSE_LITERAL.equals(getEffectiveExpression(value.get()).toSourceCode().trim())) {
            context.reportIssue(verifyHostName.get().location(), getRuleId());
        }
    }

    @Override
    public int getRuleId() {
        return AVOID_UNVERIFIED_SERVER_HOSTNAMES.getId();
    }

    @Override
    public boolean isApplicable(EmailClientConfigContext context) {
        return context.hasClientConfig();
    }
}
