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

import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportOrgNameNode;
import io.ballerina.compiler.syntax.tree.ImportPrefixNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.QualifiedNameReferenceNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.DocumentId;
import io.ballerina.projects.Module;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.Reporter;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailRule.AVOID_UNVERIFIED_SERVER_HOSTNAMES;

public class EmailSmtpClientAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final Reporter reporter;
    private static final String BALLERINA_ORG = "ballerina";
    private static final String EMAIL = "email";
    private static final String SMTP_CLIENT = "SmtpClient";
    private static final String POP_CLIENT = "PopClient";
    private static final String IMAP_CLIENT = "ImapClient";
    private static final String SECURE_SOCKET = "secureSocket";
    private static final String VERIFY_HOST_NAME = "verifyHostName";
    private final Set<String> emailPrefixes = new HashSet<>();

    public EmailSmtpClientAnalyzer(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        analyzeImports(context);
        Optional<VariableDeclarationNode> smtpClientNode = findSmtpClientNode((FunctionBodyBlockNode) context.node());
        if (smtpClientNode.isEmpty()) {
            return;
        }
        Optional<FunctionArgumentNode> smtpConfigArgument = findSmtpConfigArgument(smtpClientNode.get());
        if (smtpConfigArgument.isEmpty()) {
            return;
        }
        Optional<MappingConstructorExpressionNode> smtpConfigMapping =
                extractSmtpConfigMapping(context, smtpConfigArgument.get());
        if (smtpConfigMapping.isPresent() && isVulnerableSmtpConfig(smtpConfigMapping.get())) {
            report(context, AVOID_UNVERIFIED_SERVER_HOSTNAMES.getId());
        }
    }

    /**
     * Extracts the SMTP configuration mapping from the function argument node.
     *
     * @param context            the analysis context
     * @param smtpConfigArgument the SMTP config argument node
     * @return an Optional containing the MappingConstructorExpressionNode if found, otherwise empty
     */
    private Optional<MappingConstructorExpressionNode> extractSmtpConfigMapping(
            SyntaxNodeAnalysisContext context, FunctionArgumentNode smtpConfigArgument) {
        Optional<ExpressionNode> smtpConfigExpression = extractSmtpConfigExpressionFromFunctionBody(smtpConfigArgument);
        if (smtpConfigExpression.isEmpty()) {
            return Optional.empty();
        }
        ExpressionNode expr = smtpConfigExpression.get();
        if (expr instanceof MappingConstructorExpressionNode mappingNode) {
            return Optional.of(mappingNode);
        }
        // Try to resolve from function body block
        if (context.node() instanceof FunctionBodyBlockNode functionBodyBlockNode) {
            Optional<MappingConstructorExpressionNode> mapping =
                    extractSmtpConfigExpressionFromFunctionBody(functionBodyBlockNode, expr);
            if (mapping.isPresent()) {
                return mapping;
            }
        }
        // Try to resolve from module part
        Node parent = context.node();
        while (parent != null) {
            if (parent instanceof ModulePartNode modulePartNode) {
                Optional<MappingConstructorExpressionNode> mapping =
                        extractSmtpConfigExpressionFromModulePart(modulePartNode, expr);
                if (mapping.isPresent()) {
                    return mapping;
                }
            }
            parent = parent.parent();
        }
        return Optional.empty();
    }

    /**
     * Extracts the MappingConstructorExpressionNode for the SMTP configuration from the function body block.
     *
     * @param functionBodyBlockNode the function body block node
     * @param expressionNode        the expression node representing the SMTP configuration
     * @return an Optional containing the MappingConstructorExpressionNode if found, otherwise empty
     */
    private Optional<MappingConstructorExpressionNode> extractSmtpConfigExpressionFromFunctionBody(
            FunctionBodyBlockNode functionBodyBlockNode, ExpressionNode expressionNode) {
        for (StatementNode statement : functionBodyBlockNode.statements()) {
            if (statement instanceof VariableDeclarationNode variableDeclarationNode) {
                TypedBindingPatternNode typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
                if (typedBindingPatternNode.bindingPattern() instanceof
                        CaptureBindingPatternNode captureBindingPatternNode
                        && captureBindingPatternNode.variableName().text().equals(expressionNode.toString())
                        && variableDeclarationNode.initializer().isPresent()
                        && variableDeclarationNode.initializer().get() instanceof
                        MappingConstructorExpressionNode mappingConstructorExpressionNode) {
                    return Optional.of(mappingConstructorExpressionNode);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Extracts the MappingConstructorExpressionNode for the SMTP configuration from the module part node.
     *
     * @param modulePartNode the module part node
     * @param expressionNode the expression node representing the SMTP configuration
     * @return an Optional containing the MappingConstructorExpressionNode if found, otherwise empty
     */
    private Optional<MappingConstructorExpressionNode> extractSmtpConfigExpressionFromModulePart(
            ModulePartNode modulePartNode, ExpressionNode expressionNode) {
        for (ModuleMemberDeclarationNode statement : modulePartNode.members()) {
            if (statement instanceof ModuleVariableDeclarationNode variableDeclarationNode) {
                TypedBindingPatternNode typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
                if (typedBindingPatternNode.bindingPattern() instanceof
                        CaptureBindingPatternNode captureBindingPatternNode
                        && captureBindingPatternNode.variableName().text().equals(expressionNode.toString())
                        && variableDeclarationNode.initializer().isPresent()
                        && variableDeclarationNode.initializer().get() instanceof
                        MappingConstructorExpressionNode mappingConstructorExpressionNode) {
                    return Optional.of(mappingConstructorExpressionNode);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the SmtpClient variable declaration node in the function body block.
     *
     * @param functionBodyBlockNode the function body block node
     * @return an Optional containing the VariableDeclarationNode if found, otherwise empty
     */
    private Optional<VariableDeclarationNode> findSmtpClientNode(FunctionBodyBlockNode functionBodyBlockNode) {
        if (functionBodyBlockNode.statements().isEmpty()) {
            return Optional.empty();
        }

        for (StatementNode statement : functionBodyBlockNode.statements()) {
            if (statement instanceof VariableDeclarationNode variableDeclarationNode) {
                TypedBindingPatternNode typedBindingPatternNode = variableDeclarationNode.typedBindingPattern();
                if (!(typedBindingPatternNode.typeDescriptor() instanceof
                        QualifiedNameReferenceNode qualifiedNameReferenceNode)) {
                    continue;
                }
                if (emailPrefixes.contains(qualifiedNameReferenceNode.modulePrefix().text())
                        && (qualifiedNameReferenceNode.identifier().text().equals(SMTP_CLIENT)
                        || qualifiedNameReferenceNode.identifier().text().equals(POP_CLIENT)
                        || qualifiedNameReferenceNode.identifier().text().equals(IMAP_CLIENT))) {
                    return Optional.of(variableDeclarationNode);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Finds the SMTP configuration argument in the SmtpClient variable declaration node.
     *
     * @param variableDeclarationNode the variable declaration node
     * @return an Optional containing the FunctionArgumentNode if found, otherwise empty
     */
    private Optional<FunctionArgumentNode> findSmtpConfigArgument(VariableDeclarationNode variableDeclarationNode) {
        if (variableDeclarationNode.initializer().isEmpty()) {
            return Optional.empty();
        }

        ExpressionNode initializer = variableDeclarationNode.initializer().get();
        if (initializer instanceof CheckExpressionNode checkExpressionNode) {
            ExpressionNode expression = checkExpressionNode.expression();
            if (expression instanceof ImplicitNewExpressionNode implicitNewExpressionNode) {
                Optional<ParenthesizedArgList> parenthesizedArgList = implicitNewExpressionNode.parenthesizedArgList();
                if (parenthesizedArgList.isPresent() && parenthesizedArgList.get().arguments().size() > 3) {
                    FunctionArgumentNode functionArgumentNode = parenthesizedArgList.get().arguments().get(3);
                    return Optional.of(functionArgumentNode);
                }
            }
        } else {
            return Optional.empty();
        }

        return Optional.empty();
    }

    /**
     * Extracts the SMTP configuration expression from the FunctionArgumentNode.
     *
     * @param node the FunctionArgumentNode
     * @return an Optional containing the ExpressionNode if found, otherwise empty
     */
    private Optional<ExpressionNode> extractSmtpConfigExpressionFromFunctionBody(FunctionArgumentNode node) {
        switch (node) {
            case PositionalArgumentNode positionalArgumentNode -> {
                return Optional.of(positionalArgumentNode.expression());
            }
            case NamedArgumentNode namedArgumentNode -> {
                return Optional.of(namedArgumentNode.expression());
            }
            default -> {
                return Optional.empty();
            }
        }
    }

    /**
     * Checks if the SMTP configuration is vulnerable by verifying if the secure socket configuration
     * has the verifyHostName field set to false.
     *
     * @param mappingConstructorExpressionNode the mapping constructor expression node representing the SMTP config
     * @return true if the configuration is vulnerable, false otherwise
     */
    private boolean isVulnerableSmtpConfig(MappingConstructorExpressionNode mappingConstructorExpressionNode) {
        return mappingConstructorExpressionNode.fields().stream()
                .filter(this::isSecureSocketField)
                .map(this::getSecureSocketConfig)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .anyMatch(this::hasUnverifiedHostName);
    }

    /**
     * Checks if the given MappingFieldNode is the secureSocket field.
     *
     * @param fieldNode the MappingFieldNode to check
     * @return true if it is the secureSocket field, false otherwise
     */
    private boolean isSecureSocketField(MappingFieldNode fieldNode) {
        return fieldNode instanceof SpecificFieldNode specificFieldNode
                && specificFieldNode.fieldName().toString().trim().equals(SECURE_SOCKET);
    }

    /**
     * Retrieves the secure socket configuration from the given MappingFieldNode.
     *
     * @param fieldNode the MappingFieldNode representing the secureSocket field
     * @return an Optional containing the MappingConstructorExpressionNode if present, otherwise empty
     */
    private Optional<MappingConstructorExpressionNode> getSecureSocketConfig(MappingFieldNode fieldNode) {
        if (fieldNode instanceof SpecificFieldNode specificFieldNode) {
            Optional<ExpressionNode> expressionNode = specificFieldNode.valueExpr();
            if (expressionNode.isPresent() && expressionNode.get() instanceof
                    MappingConstructorExpressionNode secureSocketConfig) {
                return Optional.of(secureSocketConfig);
            }
        }
        return Optional.empty();
    }

    /**
     * Checks if the secure socket configuration has the verifyHostName field set to false.
     *
     * @param secureSocketConfig the MappingConstructorExpressionNode representing the secure socket configuration
     * @return true if the verifyHostName field is set to false, false otherwise
     */
    private boolean hasUnverifiedHostName(MappingConstructorExpressionNode secureSocketConfig) {
        return secureSocketConfig.fields().stream()
                .filter(this::isVerifyHostNameField)
                .map(this::getVerifyHostNameValue)
                .anyMatch(opt -> opt.isPresent() && ((BasicLiteralNode) opt.get())
                        .literalToken().text().equals("false"));
    }

    /**
     * Checks if the given MappingFieldNode is the verifyHostName field.
     *
     * @param fieldNode the MappingFieldNode to check
     * @return true if it is the verifyHostName field, false otherwise
     */
    private boolean isVerifyHostNameField(MappingFieldNode fieldNode) {
        return fieldNode instanceof SpecificFieldNode specificFieldNode
                && specificFieldNode.fieldName().toString().trim().equals(VERIFY_HOST_NAME);
    }

    /**
     * Retrieves the value of the verifyHostName field from the given MappingFieldNode.
     *
     * @param fieldNode the MappingFieldNode representing the verifyHostName field
     * @return an Optional containing the ExpressionNode if present, otherwise empty
     */
    private Optional<ExpressionNode> getVerifyHostNameValue(MappingFieldNode fieldNode) {
        if (fieldNode instanceof SpecificFieldNode specificFieldNode) {
            return specificFieldNode.valueExpr();
        }
        return Optional.empty();
    }

    /**
     * Reports an issue for the given context and rule ID.
     *
     * @param context the syntax node analysis context
     * @param ruleId  the ID of the rule to report
     */
    private void report(SyntaxNodeAnalysisContext context, int ruleId) {
        reporter.reportIssue(
                getDocument(context.currentPackage().module(context.moduleId()), context.documentId()),
                context.node().location(),
                ruleId
        );
    }

    /**
     * Retrieves the Document corresponding to the given module and document ID.
     *
     * @param module     the module
     * @param documentId the document ID
     * @return the Document for the given module and document ID
     */
    private static Document getDocument(Module module, DocumentId documentId) {
        return module.document(documentId);
    }

    /**
     * Analyzes imports to identify all prefixes used for the email module.
     *
     * @param context the syntax node analysis context
     */
    private void analyzeImports(SyntaxNodeAnalysisContext context) {
        Document document = getDocument(context.currentPackage().module(context.moduleId()), context.documentId());

        if (document.syntaxTree().rootNode() instanceof ModulePartNode modulePartNode) {
            modulePartNode.imports().forEach(importDeclarationNode -> {
                ImportOrgNameNode importOrgNameNode = importDeclarationNode.orgName().orElse(null);

                if (importOrgNameNode != null && BALLERINA_ORG.equals(importOrgNameNode.orgName().text())
                        && importDeclarationNode.moduleName().stream()
                        .anyMatch(moduleNameNode -> EMAIL.equals(moduleNameNode.text()))) {
                    ImportPrefixNode importPrefixNode = importDeclarationNode.prefix().orElse(null);
                    String prefix = importPrefixNode != null ? importPrefixNode.prefix().text() : EMAIL;
                    emailPrefixes.add(prefix);
                }
            });
        }
    }
}
