/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.email.compiler;

import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;

import java.util.Optional;

import static org.ballerinalang.stdlib.email.util.EmailConstants.EMAIL_MESSAGE;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ERROR;
import static org.ballerinalang.stdlib.email.util.EmailConstants.MODULE_NAME;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_CLOSE;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_ERROR;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_MESSAGE;

/**
 * Validate Email Services.
 */
public class EmailServiceValidator implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private FunctionDefinitionNode onMessageFunctionNode;
    private FunctionDefinitionNode onCloseFunctionNode;
    private FunctionDefinitionNode onErrorFunctionNode;
    private String modulePrefix;
    private SyntaxNodeAnalysisContext ctx;

    public static final String CODE_101 = "EMAIL_101";
    public static final String CODE_102 = "EMAIL_102";
    public static final String CODE_103 = "EMAIL_103";
    public static final String CODE_104 = "EMAIL_104";
    public static final String CODE_105 = "EMAIL_105";
    public static final String CODE_106 = "EMAIL_106";
    public static final String CODE_107 = "EMAIL_107";
    public static final String SERVICE_MUST_CONTAIN_ON_MESSAGE_FUNCTION
            = "Service must contain `onMessage` function.";
    public static final String NO_PARAMETER_PROVIDED_FOR_0_FUNCTION_EXPECTS_1_AS_A_PARAMETER
            = "No parameter provided for `{0}`, function expects `{1}` as a parameter.";
    public static final String REMOTE_KEYWORD_EXPECTED_IN_0_FUNCTION_SIGNATURE
            = "`remote` keyword expected in `{0}` function signature.";
    public static final String RESOURCE_KEYWORD_NOT_EXPECTED_IN_0_FUNCTION_SIGNATURE
            = "`resource` keyword not expected in `{0}` function signature.";
    public static final String INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2
            = "Invalid parameter `{0}` provided for `{1}`, function expects `{2}`.";
    public static final String INVALID_RETURN_TYPE_0_FUNCTION_1_RETURN_TYPE_SHOULD_BE_A_SUBTYPE_OF_2
            = "Invalid return type `{0}` provided for function `{1}`, return type should be a subtype of `{2}`";
    public static final String FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE = "Function `{0}` not accepted by the service";
    public static final String NILL = "()";

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        SeparatedNodeList<ExpressionNode> expressions = serviceDeclarationNode.expressions();

        String modulePrefix = MODULE_NAME;
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (importDeclaration.moduleName().get(0).toString().split(" ")[0]
                    .compareTo(MODULE_NAME) == 0) {
                if (importDeclaration.prefix().isPresent()) {
                    modulePrefix = importDeclaration.prefix().get().children().get(1).toString();
                }
                break;
            }
        }

        for (ExpressionNode expressionNode : expressions) {
            if (expressionNode.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {
                TypeDescriptorNode typeDescriptorNode = ((ExplicitNewExpressionNode) expressionNode).typeDescriptor();
                Node moduleIdentifierTokenOfListener = typeDescriptorNode.children().get(0);
                if (moduleIdentifierTokenOfListener.toString().compareTo(modulePrefix) == 0) {
                    this.ctx = ctx;
                    this.modulePrefix = modulePrefix + SyntaxKind.COLON_TOKEN.stringValue();
                    this.validate();
                }
            }
        }

    }

    public void validate() {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        serviceDeclarationNode.members().stream().filter(this::isResourceOrFunction).forEach(node -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String functionName = functionDefinitionNode.functionName().toString();
            checkOnResourceFunctionExistence(functionDefinitionNode, functionName);
            Boolean isRemoteFunction = isRemoteFunction(functionDefinitionNode);
            Boolean notOnMessage = (functionName.compareTo(ON_MESSAGE) != 0);
            Boolean notOnError = (functionName.compareTo(ON_ERROR) != 0);
            Boolean notOnClose = (functionName.compareTo(ON_CLOSE) != 0);
            if (isRemoteFunction && notOnMessage && notOnError && notOnClose) {
                reportInvalidFunction(functionDefinitionNode);
            } else {
                onMessageFunctionNode = functionName.compareTo(ON_MESSAGE) == 0 ? functionDefinitionNode
                        : onMessageFunctionNode;
                onCloseFunctionNode = functionName.compareTo(ON_CLOSE) == 0 ? functionDefinitionNode
                        : onCloseFunctionNode;
                onErrorFunctionNode = functionName.compareTo(ON_ERROR) == 0 ? functionDefinitionNode
                        : onErrorFunctionNode;
            }
        });
        checkOnMessageFunctionExistence();
        validateFunctionSignature(onMessageFunctionNode, ON_MESSAGE);
        validateFunctionSignature(onCloseFunctionNode, ON_CLOSE);
        validateFunctionSignature(onErrorFunctionNode, ON_ERROR);
    }

    private void checkOnResourceFunctionExistence(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        boolean hasResourceKeyword = functionDefinitionNode.qualifierList().stream()
                .filter(qualifier -> qualifier.kind() == SyntaxKind.RESOURCE_KEYWORD).toArray().length == 1;
        if (hasResourceKeyword) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE_105,
                    RESOURCE_KEYWORD_NOT_EXPECTED_IN_0_FUNCTION_SIGNATURE,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionKeyword().location(), functionName));
        }
    }

    private boolean isResourceOrFunction(Node node) {
        boolean isResource = node.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION;
        boolean isFunction = node.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION;
        return isResource || isFunction;
    }

    private boolean isRemoteFunction(FunctionDefinitionNode functionDefinitionNode) {
        return functionDefinitionNode.qualifierList().stream()
                .filter(qualifier -> qualifier.kind() == SyntaxKind.REMOTE_KEYWORD).toArray().length == 1;
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE_103, FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE,
                DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString()));
    }

    private void validateFunctionSignature(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        if (functionDefinitionNode != null) {
            checkRemoteKeywords(functionDefinitionNode, functionName);
            SeparatedNodeList<ParameterNode> parameterNodes = functionDefinitionNode.functionSignature().parameters();
            if (hasNoParameters(parameterNodes, functionDefinitionNode, functionName)) {
                return;
            }
            validateParameter(parameterNodes, functionName);
            validateFunctionReturnTypeDesc(functionDefinitionNode, functionName);
        }
    }

    private void checkOnMessageFunctionExistence() {
        if (onMessageFunctionNode == null) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE_102,
                    SERVICE_MUST_CONTAIN_ON_MESSAGE_FUNCTION, DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    ctx.node().location()));
        }
    }

    private void checkRemoteKeywords(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        if (!isRemoteFunction(functionDefinitionNode)) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE_101,
                    REMOTE_KEYWORD_EXPECTED_IN_0_FUNCTION_SIGNATURE,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionKeyword().location(), functionName));
        }
    }

    private boolean hasNoParameters(SeparatedNodeList<ParameterNode> parameterNodes,
                                    FunctionDefinitionNode functionDefinitionNode,
                                    String functionName) {
        if (parameterNodes.isEmpty()) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE_106,
                    NO_PARAMETER_PROVIDED_FOR_0_FUNCTION_EXPECTS_1_AS_A_PARAMETER, DiagnosticSeverity.ERROR);
            String expectedParameter = functionName.equals(ON_MESSAGE) ?
                    modulePrefix + EMAIL_MESSAGE : functionName.equals(ON_ERROR) ?
                    modulePrefix + ERROR : modulePrefix + ON_CLOSE;
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionSignature().location(), functionName, expectedParameter));
            return true;
        }
        return false;
    }

    private void validateParameter(SeparatedNodeList<ParameterNode> parameterNodes, String functionName) {
        for (ParameterNode parameterNode : parameterNodes) {
            RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
            Node parameterTypeName = requiredParameterNode.typeName();
            DiagnosticInfo diagnosticInfo;
            if (functionName.equals(ON_MESSAGE) && (!parameterTypeName.toString().contains(EMAIL_MESSAGE)
                    || !parameterTypeName.toString().contains(MODULE_NAME))) {
                diagnosticInfo = new DiagnosticInfo(CODE_104,
                        INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode, functionName,
                        modulePrefix + EMAIL_MESSAGE));
            } else if (functionName.equals(ON_ERROR) && !parameterTypeName.toString().contains(ERROR)) {
                diagnosticInfo = new DiagnosticInfo(CODE_104,
                        INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode, functionName,
                        modulePrefix + ERROR));
            } else if (functionName.equals(ON_CLOSE) && !parameterTypeName.toString().contains(ERROR)) {
                diagnosticInfo = new DiagnosticInfo(CODE_104,
                        INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2, DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode, functionName,
                        modulePrefix + ERROR));
            }
        }
    }

    private void validateFunctionReturnTypeDesc(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        Optional<ReturnTypeDescriptorNode> returnTypeDescriptorNode = functionDefinitionNode
                .functionSignature().returnTypeDesc();
        if (returnTypeDescriptorNode.isEmpty()) {
            return;
        }
        Node returnTypeDescriptor = returnTypeDescriptorNode.get().type();
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE_107,
                INVALID_RETURN_TYPE_0_FUNCTION_1_RETURN_TYPE_SHOULD_BE_A_SUBTYPE_OF_2, DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                returnTypeDescriptor.location(), returnTypeDescriptor.toString(), functionName, NILL));
    }

}
