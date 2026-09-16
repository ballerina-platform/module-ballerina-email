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

import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.Reporter;

import java.util.Optional;
import java.util.Set;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getDocument;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getEmailClientTypeName;

/**
 * Analyzes the construction of {@code ballerina/email} clients.
 * <p>
 * Transport security is decided at construction, so this task registers on the constructor expression itself. That
 * reaches every client, including one built at module level or returned directly, rather than only those declared
 * as a variable inside a function body.
 */
public class EmailClientConfigAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private static final Set<String> CLIENT_TYPES = Set.of("SmtpClient", "PopClient", "ImapClient");

    private final Reporter reporter;
    private final EmailClientConfigRulesEngine rulesEngine;

    public EmailClientConfigAnalyzer(Reporter reporter) {
        this.reporter = reporter;
        this.rulesEngine = new EmailClientConfigRulesEngine();
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        if (!(context.node() instanceof NewExpressionNode newExpression)) {
            return;
        }
        Optional<String> clientTypeName = getEmailClientTypeName(context.semanticModel(), newExpression);
        if (clientTypeName.isEmpty() || !CLIENT_TYPES.contains(clientTypeName.get())) {
            return;
        }
        rulesEngine.executeRules(new EmailClientConfigContext(reporter, getDocument(context), clientTypeName.get(),
                newExpression));
    }
}
