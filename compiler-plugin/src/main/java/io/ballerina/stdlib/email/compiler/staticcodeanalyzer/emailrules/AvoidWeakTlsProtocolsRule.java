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

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.findField;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getListElements;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getStringLiteralValue;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailRule.AVOID_WEAK_TLS_PROTOCOLS;

/**
 * Rule to detect weak TLS protocol versions named on a mail client.
 * <p>
 * TLS 1.0 and TLS 1.1 are withdrawn: both rely on MD5 and SHA-1 in the handshake and lack the modern cipher suites,
 * which leaves them open to downgrade and padding-oracle attacks. SSL 3.0 is broken outright. Naming any of them
 * pins the connection to a protocol version a current server should refuse.
 */
public class AvoidWeakTlsProtocolsRule implements EmailClientConfigRule {

    private static final String SECURE_SOCKET = "secureSocket";
    private static final String PROTOCOL = "protocol";
    private static final String VERSIONS = "versions";
    private static final Set<String> WEAK_VERSIONS = Set.of("SSLV3", "SSLV2", "TLSV1.0", "TLSV1.1", "TLSV1", "SSL");

    @Override
    public void analyze(EmailClientConfigContext context) {
        Optional<SpecificFieldNode> versions = context.getNestedConfigRecord(SECURE_SOCKET, PROTOCOL)
                .flatMap(protocol -> findField(protocol, VERSIONS));
        if (versions.isEmpty() || versions.get().valueExpr().isEmpty()) {
            return;
        }
        for (ExpressionNode version : getListElements(versions.get().valueExpr().get())) {
            boolean isWeak = getStringLiteralValue(version)
                    .map(value -> WEAK_VERSIONS.contains(value.trim().toUpperCase(Locale.ROOT)))
                    .orElse(false);
            if (isWeak) {
                context.reportIssue(version.location(), getRuleId());
            }
        }
    }

    @Override
    public int getRuleId() {
        return AVOID_WEAK_TLS_PROTOCOLS.getId();
    }

    @Override
    public boolean isApplicable(EmailClientConfigContext context) {
        return context.hasClientConfig();
    }
}
