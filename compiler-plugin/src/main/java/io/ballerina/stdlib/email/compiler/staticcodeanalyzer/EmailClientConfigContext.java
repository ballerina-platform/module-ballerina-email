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
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.projects.Document;
import io.ballerina.scan.Reporter;
import io.ballerina.tools.diagnostics.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.findField;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.getArguments;
import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailAnalysisUtils.resolveConfigRecord;

/**
 * Represents the configuration of an email client being constructed.
 * <p>
 * The three client types take their configuration through the same shaped record, so the configuration is resolved
 * once here and every rule reads it the same way regardless of which client is being built.
 * <p>
 * {@code clientConfig} is an included record parameter, so a caller may pass the record whole, positionally or by
 * name, or flatten any of its fields into named arguments of their own. Named arguments may also appear in any
 * order. Both forms describe the same configuration, so both are collected here and a rule asks for a field without
 * having to know how it was written.
 */
public class EmailClientConfigContext {

    private static final String CLIENT_CONFIG_PARAM = "clientConfig";
    private static final int CLIENT_CONFIG_POSITION = 3;
    // The parameters declared ahead of the included record parameter. Every other named argument names one of the
    // configuration record's own fields.
    private static final Set<String> FIXED_PARAMS = Set.of("host", "username", "password", CLIENT_CONFIG_PARAM);

    private final Reporter reporter;
    private final Document document;
    private final String clientTypeName;
    private final Location constructionLocation;
    private final MappingConstructorExpressionNode clientConfig;
    private final Map<String, NamedArgumentNode> flattenedFields;

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
        SeparatedNodeList<FunctionArgumentNode> arguments = getArguments(newExpression).orElse(null);
        this.clientConfig = arguments == null ? null : resolveClientConfig(arguments);
        this.flattenedFields = arguments == null ? Map.of() : resolveFlattenedFields(arguments);
    }

    /**
     * Resolve the configuration record passed whole, either as the named {@code clientConfig} argument or in the
     * position the parameter is declared at.
     */
    private static MappingConstructorExpressionNode resolveClientConfig(
            SeparatedNodeList<FunctionArgumentNode> arguments) {
        int positionalIndex = 0;
        for (FunctionArgumentNode argument : arguments) {
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
     * Collect the configuration fields flattened into named arguments of their own. Anything named that is not one
     * of the parameters declared ahead of the included record parameter is such a field.
     */
    private static Map<String, NamedArgumentNode> resolveFlattenedFields(
            SeparatedNodeList<FunctionArgumentNode> arguments) {
        Map<String, NamedArgumentNode> fields = new HashMap<>();
        for (FunctionArgumentNode argument : arguments) {
            if (argument instanceof NamedArgumentNode namedArgument) {
                String argumentName = namedArgument.argumentName().name().text();
                if (!FIXED_PARAMS.contains(argumentName)) {
                    fields.put(argumentName, namedArgument);
                }
            }
        }
        return fields;
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
     * Whether any client configuration could be resolved, whether passed as a record or flattened into named
     * arguments.
     *
     * @return true if a configuration is available
     */
    public boolean hasClientConfig() {
        return this.clientConfig != null || !this.flattenedFields.isEmpty();
    }

    /**
     * Find the node carrying a configuration field, which is a named argument when the field was flattened into one
     * and a record field otherwise. A flattened field takes precedence, since a call carrying one cannot also pass
     * the record whole.
     *
     * @param fieldName the field name to look for
     * @return the node carrying the field if the configuration has it, empty otherwise
     */
    private Optional<Node> findConfigFieldNode(String fieldName) {
        NamedArgumentNode flattenedField = this.flattenedFields.get(fieldName);
        if (flattenedField != null) {
            return Optional.of(flattenedField);
        }
        return this.clientConfig == null ? Optional.empty()
                : findField(this.clientConfig, fieldName).map(Node.class::cast);
    }

    /**
     * Follow a chain of nested records within the client configuration. Each step resolves a variable reference to
     * its declaration, so a record held in a variable is followed like one written inline.
     *
     * @param fieldNames the field names to follow, outermost first
     * @return the innermost record if the whole chain is present, empty otherwise
     */
    public Optional<MappingConstructorExpressionNode> getNestedConfigRecord(String... fieldNames) {
        if (fieldNames.length == 0) {
            return Optional.ofNullable(this.clientConfig);
        }
        Optional<MappingConstructorExpressionNode> current = getConfigFieldValue(fieldNames[0])
                .flatMap(EmailAnalysisUtils::resolveConfigRecord);
        for (int index = 1; index < fieldNames.length; index++) {
            String fieldName = fieldNames[index];
            current = current.flatMap(record -> findField(record, fieldName))
                    .flatMap(SpecificFieldNode::valueExpr)
                    .flatMap(EmailAnalysisUtils::resolveConfigRecord);
        }
        return current;
    }

    /**
     * Get the value of a field of the client configuration, however the field was written.
     *
     * @param fieldName the field name to look for
     * @return the field's value if present, empty otherwise
     */
    public Optional<ExpressionNode> getConfigFieldValue(String fieldName) {
        return findConfigFieldNode(fieldName).flatMap(node -> switch (node) {
            case NamedArgumentNode namedArgument -> Optional.of(namedArgument.expression());
            case SpecificFieldNode field -> field.valueExpr();
            default -> Optional.empty();
        });
    }

    /**
     * Report an issue against a field of the client configuration, at wherever that field was written.
     *
     * @param fieldName the field the issue is about
     * @param ruleId    the rule reporting the issue
     */
    public void reportConfigFieldIssue(String fieldName, int ruleId) {
        findConfigFieldNode(fieldName).ifPresent(node -> reportIssue(node.location(), ruleId));
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
