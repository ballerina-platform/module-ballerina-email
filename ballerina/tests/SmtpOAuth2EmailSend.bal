// Copyright (c) 2026 WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/jballerina.java;
import ballerina/oauth2;
import ballerina/test;

@test:Config {
    groups: ["smtp", "oauth2"]
}
function testSendEmailWithOAuth2() returns error? {
    check startOAuth2SmtpServer();
    do {
        SmtpConfiguration smtpConfig = {
            port: 3586,
            security: START_TLS_AUTO,
            auth: <oauth2:ClientCredentialsGrantConfig>{
                tokenUrl: "http://127.0.0.1:9099/token",
                clientId: "test-client-id",
                clientSecret: "test-client-secret"
            }
        };
        SmtpClient smtpClient = check new ("127.0.0.1", "someone@localhost.com", clientConfig = smtpConfig);
        check smtpClient->sendMessage({
            to: "hascode@localhost",
            subject: "OAuth2 Test Email",
            body: "This is a test email sent via XOAUTH2."
        });
        check validateOAuth2Email();
    } on fail var e {
        check stopOAuth2SmtpServer();
        return e;
    }
}

@test:Config {
    groups: ["smtp", "oauth2"],
    dependsOn: [testSendEmailWithOAuth2]
}
function testSendEmailWithOAuth2WrongCredentials() returns error? {
    SmtpConfiguration smtpConfig = {
        port: 3586,
        security: START_TLS_AUTO,
        auth: <oauth2:ClientCredentialsGrantConfig>{
            tokenUrl: "http://127.0.0.1:9099/token",
            clientId: "wrong-client-id",
            clientSecret: "wrong-client-secret"
        }
    };
    // ClientOAuth2Provider.init panics on token fetch failure; trap converts that to an error.
    SmtpClient|error clientResult = trap new ("127.0.0.1", "someone@localhost.com", clientConfig = smtpConfig);
    if clientResult is error {
        // Error during token acquisition at init time is also acceptable
        check stopOAuth2SmtpServer();
        return;
    }
    Error? sendResult = clientResult->sendMessage({
        to: "hascode@localhost",
        subject: "Should fail",
        body: "This email should not be sent."
    });
    check stopOAuth2SmtpServer();
    if sendResult is () {
        test:assertFail("Expected an error when using wrong OAuth2 credentials");
    }
}

@test:Config {}
function testOAuth2InitRequiresUsername() returns error? {
    SmtpConfiguration smtpConfig = {
        port: 3586,
        security: START_TLS_AUTO,
        auth: <oauth2:ClientCredentialsGrantConfig>{
            tokenUrl: "http://127.0.0.1:9099/token",
            clientId: "test-client-id",
            clientSecret: "test-client-secret"
        }
    };
    SmtpClient|Error result = new ("127.0.0.1", clientConfig = smtpConfig);
    if result is SmtpClient {
        test:assertFail("Expected error when OAuth2 is configured without a username");
    }
    test:assertTrue(result.message().includes("Invalid configuration"),
        msg = "Expected 'Invalid configuration' error when username is missing with OAuth2");
}

public function startOAuth2SmtpServer() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpOAuth2EmailSendTest"
} external;

public function stopOAuth2SmtpServer() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpOAuth2EmailSendTest"
} external;

public function validateOAuth2Email() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpOAuth2EmailSendTest"
} external;
