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

import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.Reporter;
import io.ballerina.tools.diagnostics.Location;

import java.util.Optional;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.findField;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getArguments;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getNestedRecord;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.resolveConfigRecord;

/**
 * Represents the configuration of an email client being constructed.
 * <p>
 * The three client types take their configuration through the same shaped record, so the record is resolved once
 * here and every rule reads it the same way regardless of which client is being built.
 */
public class EmailClientConfigContext {

    private static final String CLIENT_CONFIG_PARAM = "clientConfig";
    private static final int CLIENT_CONFIG_POSITION = 3;

    private final Reporter reporter;
    private final Document document;
    private final String clientTypeName;
    private final Location constructionLocation;
    private final MappingConstructorExpressionNode clientConfig;

    /**
     * Creates a context for the given email client construction.
     *
     * @param reporter       the static code analysis reporter
     * @param document       the document containing the construction
     * @param clientTypeName the simple name of the email client type being constructed
     * @param newExpression  the constructor expression being analyzed
     */
    public EmailClientConfigContext(Reporter reporter, Document document, String clientTypeName,
                                    NewExpressionNode newExpression) {
        this.reporter = reporter;
        this.document = document;
        this.clientTypeName = clientTypeName;
        this.constructionLocation = newExpression.location();
        this.clientConfig = resolveClientConfig(newExpression);
    }

    private static MappingConstructorExpressionNode resolveClientConfig(NewExpressionNode newExpression) {
        Optional<SeparatedNodeList<FunctionArgumentNode>> arguments = getArguments(newExpression);
        if (arguments.isEmpty()) {
            return null;
        }
        int positionalIndex = 0;
        for (FunctionArgumentNode argument : arguments.get()) {
            switch (argument) {
                case NamedArgumentNode namedArgument -> {
                    if (CLIENT_CONFIG_PARAM.equals(namedArgument.argumentName().name().text())) {
                        return resolveConfigRecord(namedArgument.expression()).orElse(null);
                    }
                }
                case PositionalArgumentNode positionalArgument -> {
                    if (positionalIndex++ == CLIENT_CONFIG_POSITION) {
                        return resolveConfigRecord(positionalArgument.expression()).orElse(null);
                    }
                }
                default -> {
                    // A rest argument spreads a value that cannot be resolved without data-flow analysis
                }
            }
        }
        return null;
    }

    /**
     * The simple name of the email client type being constructed, such as {@code SmtpClient}.
     *
     * @return the constructed client type's name
     */
    public String getClientTypeName() {
        return this.clientTypeName;
    }

    /**
     * The location of the whole construction.
     *
     * @return the location of the constructor expression
     */
    public Location getConstructionLocation() {
        return this.constructionLocation;
    }

    /**
     * Whether the client configuration record could be resolved.
     *
     * @return true if a configuration record is available
     */
    public boolean hasClientConfig() {
        return this.clientConfig != null;
    }

    /**
     * Get a field of the client configuration record.
     *
     * @param fieldName the field name to look for
     * @return the field if the record was resolved and carries it, empty otherwise
     */
    public Optional<SpecificFieldNode> getConfigField(String fieldName) {
        return this.clientConfig == null ? Optional.empty() : findField(this.clientConfig, fieldName);
    }

    /**
     * Follow a chain of nested records within the client configuration.
     *
     * @param fieldNames the field names to follow, outermost first
     * @return the innermost record if the whole chain is present, empty otherwise
     */
    public Optional<MappingConstructorExpressionNode> getNestedConfigRecord(String... fieldNames) {
        Optional<MappingConstructorExpressionNode> current = Optional.ofNullable(this.clientConfig);
        for (String fieldName : fieldNames) {
            current = current.flatMap(record -> getNestedRecord(record, fieldName));
        }
        return current;
    }

    /**
     * Get the value of a field of the client configuration record.
     *
     * @param fieldName the field name to look for
     * @return the field's value if present, empty otherwise
     */
    public Optional<ExpressionNode> getConfigFieldValue(String fieldName) {
        return getConfigField(fieldName).flatMap(SpecificFieldNode::valueExpr);
    }

    /**
     * Report an issue against this construction.
     *
     * @param location the location to report at
     * @param ruleId   the rule reporting the issue
     */
    public void reportIssue(Location location, int ruleId) {
        this.reporter.reportIssue(this.document, location, ruleId);
    }
}
