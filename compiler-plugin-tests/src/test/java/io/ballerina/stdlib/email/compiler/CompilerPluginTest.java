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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_101;
import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_102;
import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_103;
import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_104;
import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_105;
import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_106;
import static io.ballerina.stdlib.email.compiler.EmailServiceValidator.CODE_107;

/**
 * Tests for Ballerina Email Compiler Plugin.
 */
public class CompilerPluginTest {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources", "ballerina_sources")
            .toAbsolutePath();
    private static final Path DISTRIBUTION_PATH = Paths.get("../", "target", "ballerina-runtime")
            .toAbsolutePath();

    @Test
    public void testHappyPath() {
        Package currentPackage = loadPackage("sample_package_1");
        PackageCompilation compilation = currentPackage.getCompilation();

        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testReadonlyHappyPath() {
        Package currentPackage = loadPackage("sample_package_12");
        PackageCompilation compilation = currentPackage.getCompilation();

        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    @Test
    public void testRemoteFunctionsWithoutRemoteKeyword() {
        Package currentPackage = loadPackage("sample_package_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.errors()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_101);
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    EmailServiceValidator.REMOTE_KEYWORD_EXPECTED_IN_0_FUNCTION_SIGNATURE);
        }
    }

    @Test
    public void testServiceWithoutOnMessageKeyword() {
        Package currentPackage = loadPackage("sample_package_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        for (Diagnostic diagnostic : diagnosticResult.errors()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_102);
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    EmailServiceValidator.SERVICE_MUST_CONTAIN_ON_MESSAGE_FUNCTION);
        }
    }

    @Test
    public void testWithUnSupportedFunctionNames() {
        Package currentPackage = loadPackage("sample_package_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 2);
        for (Diagnostic diagnostic : diagnosticResult.errors()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_103);
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    EmailServiceValidator.FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE);
        }
    }

    @Test
    public void testWithUnSupportedParameters() {
        Package currentPackage = loadPackage("sample_package_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.errors()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_104);
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    EmailServiceValidator.INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2);
        }
    }

    @Test
    public void testFunctionsWithResourceKeyword() {
        Package currentPackage = loadPackage("sample_package_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.errors().toArray()[0];
        Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_105);
        Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                EmailServiceValidator.RESOURCE_KEYWORD_NOT_EXPECTED_IN_0_FUNCTION_SIGNATURE);
    }

    @Test
    public void testWithEmptyParameters() {
        Package currentPackage = loadPackage("sample_package_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.errors()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_106);
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    EmailServiceValidator.NO_PARAMETER_PROVIDED_FOR_0_FUNCTION_EXPECTS_1_AS_A_PARAMETER);
        }
    }

    @Test
    public void testWithInvalidReturnType() {
        Package currentPackage = loadPackage("sample_package_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.errors()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().code(), CODE_107);
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    EmailServiceValidator.INVALID_RETURN_TYPE_0_FUNCTION_1_RETURN_TYPE_SHOULD_BE_A_SUBTYPE_OF_2);
        }
    }

    @Test
    public void testListenerPreDeclaredListenerWithWrongReturnTypes() {
        Package currentPackage = loadPackage("sample_package_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 1);
    }

    @Test
    public void testListenerPreDeclaredListener() {
        Package currentPackage = loadPackage("sample_package_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.errors().size(), 0);
    }

    private Package loadPackage(String path) {
        Path projectDirPath = RESOURCE_DIRECTORY.resolve(path);
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }

    private ProjectEnvironmentBuilder getEnvironmentBuilder() {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        return ProjectEnvironmentBuilder.getBuilder(environment);
    }

}
