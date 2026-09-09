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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.BasicLiteralNode;
import io.ballerina.compiler.syntax.tree.CaptureBindingPatternNode;
import io.ballerina.compiler.syntax.tree.CheckExpressionNode;
import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionBodyBlockNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ListConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.ModuleVariableDeclarationNode;
import io.ballerina.compiler.syntax.tree.NewExpressionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.ParenthesizedArgList;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.StatementNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeCastExpressionNode;
import io.ballerina.compiler.syntax.tree.TypedBindingPatternNode;
import io.ballerina.compiler.syntax.tree.VariableDeclarationNode;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import java.util.List;
import java.util.Optional;


/**
 * Shared helpers for the email static code analysis rules.
 */
public final class EmailAnalysisUtils {

    private static final String BALLERINA_ORG = "ballerina";
    private static final String EMAIL = "email";

    private EmailAnalysisUtils() {
    }

    /**
     * Retrieve the document being analyzed.
     *
     * @param context the syntax node analysis context
     * @return the document the analyzed node belongs to
     */
    public static Document getDocument(SyntaxNodeAnalysisContext context) {
        return context.currentPackage().module(context.moduleId()).document(context.documentId());
    }

    /**
     * Resolve the simple name of an email client type being constructed.
     *
     * @param semanticModel the semantic model
     * @param newExpression the constructor expression
     * @return the constructed type's simple name if it belongs to {@code ballerina/email}, empty otherwise
     */
    public static Optional<String> getEmailClientTypeName(SemanticModel semanticModel,
                                                          NewExpressionNode newExpression) {
        Optional<TypeSymbol> constructorType = semanticModel.typeOf(newExpression);
        if (constructorType.isEmpty()) {
            return Optional.empty();
        }
        TypeSymbol constructedType = resolveConstructedType(constructorType.get());
        Optional<ModuleSymbol> module = constructedType.getModule();
        if (module.isEmpty() || module.get().getName().isEmpty()
                || !BALLERINA_ORG.equals(module.get().id().orgName())
                || !EMAIL.equals(module.get().getName().get())) {
            return Optional.empty();
        }
        return constructedType.getName();
    }

    /**
     * Resolve the type produced by a constructor expression, unwrapping the error union a failable {@code init}
     * produces and any {@code readonly &} intersection.
     */
    private static TypeSymbol resolveConstructedType(TypeSymbol typeSymbol) {
        TypeSymbol effective = typeSymbol;
        if (effective instanceof IntersectionTypeSymbol intersectionTypeSymbol) {
            effective = intersectionTypeSymbol.effectiveTypeDescriptor();
        }
        if (effective instanceof UnionTypeSymbol unionTypeSymbol) {
            List<TypeSymbol> constructedTypes = unionTypeSymbol.memberTypeDescriptors().stream()
                    .filter(member -> !isErrorType(member))
                    .toList();
            if (constructedTypes.size() == 1) {
                effective = constructedTypes.get(0);
            }
        }
        return effective;
    }

    private static boolean isErrorType(TypeSymbol typeSymbol) {
        TypeSymbol effective = typeSymbol instanceof TypeReferenceTypeSymbol typeReferenceTypeSymbol
                ? typeReferenceTypeSymbol.typeDescriptor() : typeSymbol;
        return effective.typeKind() == TypeDescKind.ERROR;
    }

    /**
     * Get the argument list of a constructor expression. A bare {@code new} carries none.
     *
     * @param newExpression the constructor expression
     * @return the argument list if present, empty otherwise
     */
    public static Optional<SeparatedNodeList<FunctionArgumentNode>> getArguments(NewExpressionNode newExpression) {
        return switch (newExpression) {
            case ExplicitNewExpressionNode explicitNew -> Optional.of(explicitNew.parenthesizedArgList().arguments());
            case ImplicitNewExpressionNode implicitNew ->
                    implicitNew.parenthesizedArgList().map(ParenthesizedArgList::arguments);
            default -> Optional.empty();
        };
    }

    /**
     * Resolve the configuration record behind an argument, whether written inline or held in a variable declared in
     * the enclosing function or at module level.
     *
     * @param expression the argument expression
     * @return the configuration record if it can be resolved, empty otherwise
     */
    public static Optional<MappingConstructorExpressionNode> resolveConfigRecord(ExpressionNode expression) {
        ExpressionNode effective = getEffectiveExpression(expression);
        if (effective instanceof MappingConstructorExpressionNode mappingConstructor) {
            return Optional.of(mappingConstructor);
        }
        if (effective.kind() != SyntaxKind.SIMPLE_NAME_REFERENCE) {
            return Optional.empty();
        }
        String variableName = effective.toSourceCode().trim();
        Node current = effective.parent();
        while (current != null) {
            // A parameter shadows anything declared outside the function, and its value is supplied by the
            // caller. Resolving past it would read a same-named module variable and analyze a configuration the
            // call never uses.
            if (current instanceof FunctionDefinitionNode function
                    && declaresParameter(function, variableName)) {
                return Optional.empty();
            }
            Optional<MappingConstructorExpressionNode> resolved = switch (current) {
                case FunctionBodyBlockNode body -> findInStatements(body.statements(), variableName);
                case ModulePartNode modulePart -> findInModuleMembers(modulePart.members(), variableName);
                default -> Optional.empty();
            };
            if (resolved.isPresent()) {
                return resolved;
            }
            current = current.parent();
        }
        return Optional.empty();
    }

    private static boolean declaresParameter(FunctionDefinitionNode function, String variableName) {
        return function.functionSignature().parameters().stream()
                .filter(RequiredParameterNode.class::isInstance)
                .map(RequiredParameterNode.class::cast)
                .anyMatch(parameter -> parameter.paramName()
                        .map(name -> variableName.equals(name.text()))
                        .orElse(false));
    }

    private static Optional<MappingConstructorExpressionNode> findInStatements(NodeList<StatementNode> statements,
                                                                              String variableName) {
        for (StatementNode statement : statements) {
            if (statement instanceof VariableDeclarationNode declaration) {
                Optional<MappingConstructorExpressionNode> match = matchDeclaration(
                        declaration.typedBindingPattern(), declaration.initializer(), variableName);
                if (match.isPresent()) {
                    return match;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<MappingConstructorExpressionNode> findInModuleMembers(
            NodeList<ModuleMemberDeclarationNode> members, String variableName) {
        for (ModuleMemberDeclarationNode member : members) {
            if (member instanceof ModuleVariableDeclarationNode declaration) {
                Optional<MappingConstructorExpressionNode> match = matchDeclaration(
                        declaration.typedBindingPattern(), declaration.initializer(), variableName);
                if (match.isPresent()) {
                    return match;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<MappingConstructorExpressionNode> matchDeclaration(TypedBindingPatternNode bindingPattern,
                                                                              Optional<ExpressionNode> initializer,
                                                                              String variableName) {
        if (initializer.isEmpty()
                || !(bindingPattern.bindingPattern() instanceof CaptureBindingPatternNode capture)
                || !capture.variableName().text().equals(variableName)) {
            return Optional.empty();
        }
        return getEffectiveExpression(initializer.get()) instanceof MappingConstructorExpressionNode mappingConstructor
                ? Optional.of(mappingConstructor) : Optional.empty();
    }

    /**
     * Unwrap a {@code check} or a type cast to reach the expression underneath.
     *
     * @param expression the expression to unwrap
     * @return the unwrapped expression
     */
    public static ExpressionNode getEffectiveExpression(ExpressionNode expression) {
        return switch (expression) {
            case CheckExpressionNode checkExpression -> checkExpression.expression();
            case TypeCastExpressionNode castExpression -> castExpression.expression();
            default -> expression;
        };
    }

    /**
     * Find a field by name within a record. Computed and spread fields cannot be resolved statically and are
     * skipped.
     *
     * @param record    the record to search
     * @param fieldName the field name to look for
     * @return the matching field if present, empty otherwise
     */
    public static Optional<SpecificFieldNode> findField(MappingConstructorExpressionNode record, String fieldName) {
        return record.fields().stream()
                .filter(field -> field.kind() == SyntaxKind.SPECIFIC_FIELD)
                .map(field -> (SpecificFieldNode) field)
                .filter(field -> matchesFieldName(field.fieldName(), fieldName))
                .findFirst();
    }

    private static boolean matchesFieldName(Node fieldNameNode, String expectedFieldName) {
        if (fieldNameNode instanceof IdentifierToken identifierToken) {
            return identifierToken.text().equals(expectedFieldName);
        }
        if (fieldNameNode instanceof BasicLiteralNode basicLiteralNode) {
            String literal = basicLiteralNode.literalToken().text();
            return literal.substring(1, literal.length() - 1).equals(expectedFieldName);
        }
        return false;
    }

    /**
     * Get the element expressions of a list constructor.
     *
     * @param expression the expression to read
     * @return the list elements, or an empty list if the expression is not a list constructor
     */
    public static List<ExpressionNode> getListElements(ExpressionNode expression) {
        if (!(getEffectiveExpression(expression) instanceof ListConstructorExpressionNode listConstructor)) {
            return List.of();
        }
        return listConstructor.expressions().stream()
                .filter(ExpressionNode.class::isInstance)
                .map(ExpressionNode.class::cast)
                .toList();
    }

    /**
     * Get the value of a string literal expression, with the surrounding quotes removed.
     *
     * @param expression the expression to read
     * @return the literal value if the expression is a string literal, empty otherwise
     */
    public static Optional<String> getStringLiteralValue(ExpressionNode expression) {
        String source = expression.toSourceCode().trim();
        if (source.length() >= 2 && source.startsWith("\"") && source.endsWith("\"")) {
            return Optional.of(decodeEscapes(source.substring(1, source.length() - 1)));
        }
        return Optional.empty();
    }

    /**
     * Decode the escapes a Ballerina string literal may contain, so a rule compares the value the literal denotes
     * rather than the characters used to write it. Without this, a version written with its final digit as a
     * unicode escape reads as a different string from the same version written plainly, and slips past a value
     * comparison.
     */
    private static String decodeEscapes(String literal) {
        StringBuilder decoded = new StringBuilder(literal.length());
        for (int i = 0; i < literal.length(); i++) {
            char current = literal.charAt(i);
            if (current != '\\' || i + 1 >= literal.length()) {
                decoded.append(current);
                continue;
            }
            char next = literal.charAt(i + 1);
            if (next == 'u' && i + 2 < literal.length() && literal.charAt(i + 2) == '{') {
                int close = literal.indexOf('}', i + 3);
                if (close > 0) {
                    try {
                        decoded.appendCodePoint(Integer.parseInt(literal.substring(i + 3, close), 16));
                        i = close;
                        continue;
                    } catch (IllegalArgumentException e) {
                        // Not a code point this analyzer can read; keep the text as written
                    }
                }
            }
            decoded.append(switch (next) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                default -> next;
            });
            i++;
        }
        return decoded.toString();
    }

    /**
     * Check whether an expression denotes the given enum member, written either as a reference to it, with or
     * without a module prefix, or as the string it equals.
     * <p>
     * A member of these enums is declared without an explicit value, so it is the string singleton of its own name.
     * Writing that string is therefore another way of naming the same member, and one a rule has to recognise or a
     * configuration slips past by being spelled differently.
     *
     * @param expression the expression to read
     * @param memberName the enum member name
     * @return true if the expression denotes that member
     */
    public static boolean namesEnumMember(ExpressionNode expression, String memberName) {
        ExpressionNode effective = getEffectiveExpression(expression);
        Optional<String> literalValue = getStringLiteralValue(effective);
        if (literalValue.isPresent()) {
            return literalValue.get().equals(memberName);
        }
        String source = effective.toSourceCode().trim();
        return source.equals(memberName) || source.endsWith(":" + memberName);
    }
}
