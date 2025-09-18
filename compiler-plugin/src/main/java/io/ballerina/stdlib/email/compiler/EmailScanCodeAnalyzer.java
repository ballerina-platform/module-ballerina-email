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

package io.ballerina.stdlib.email.compiler;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;
import io.ballerina.scan.Reporter;
import io.ballerina.stdlib.email.compiler.staticcodeanalyzer.EmailSmtpClientAnalyzer;

/**
 * Email compiler plugin.
 *
 * @since 2.12.1
 */
public class EmailScanCodeAnalyzer extends CodeAnalyzer {
    private final Reporter reporter;

    public EmailScanCodeAnalyzer(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void init(CodeAnalysisContext codeAnalysisContext) {
        codeAnalysisContext.addSyntaxNodeAnalysisTask(new EmailSmtpClientAnalyzer(reporter),
                SyntaxKind.FUNCTION_BODY_BLOCK);
    }
}
