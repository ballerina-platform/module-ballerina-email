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
import ballerina/test;

@test:Config {
    groups: ["smtp", "oauth2"]
}
function testSendEmailWithDirectToken() returns error? {
    check startOAuth2SmtpServer();
    do {
        SmtpConfiguration smtpConfig = {
            port: 3586,
            security: START_TLS_AUTO,
            auth: "smtp_oauth2_test_token_12345"
        };
        SmtpClient smtpClient = check new ("127.0.0.1", "someone@localhost.com", clientConfig = smtpConfig);
        check smtpClient->sendMessage({
            to: "hascode@localhost",
            subject: "OAuth2 Test Email",
            body: "This is a test email sent via XOAUTH2 with a direct token."
        });
        check validateOAuth2Email();
    } on fail var e {
        check stopOAuth2SmtpServer();
        return e;
    }
}

@test:Config {
    groups: ["smtp", "oauth2"],
    dependsOn: [testSendEmailWithDirectToken]
}
function testSendEmailWithClientCredentialsGrant() returns error? {
    OAuth2ClientCredentialsGrantConfig grantConfig = {
        tokenUrl: "http://127.0.0.1:9099",
        clientId: "test-client-id",
        clientSecret: "test-client-secret"
    };
    SmtpConfiguration smtpConfig = {
        port: 3586,
        security: START_TLS_AUTO,
        auth: grantConfig
    };
    do {
        SmtpClient smtpClient = check new ("127.0.0.1", "someone@localhost.com", clientConfig = smtpConfig);
        check smtpClient->sendMessage({
            to: "hascode@localhost",
            subject: "OAuth2 Test Email",
            body: "This is a test email sent via XOAUTH2 using a Client Credentials Grant."
        });
        check validateOAuth2EmailCount(2);
    } on fail var e {
        check stopOAuth2SmtpServer();
        return e;
    }
}

@test:Config {
    groups: ["smtp", "oauth2"],
    dependsOn: [testSendEmailWithClientCredentialsGrant]
}
function testSendEmailWithInvalidClientCredentials() returns error? {
    OAuth2ClientCredentialsGrantConfig grantConfig = {
        tokenUrl: "http://127.0.0.1:9099",
        clientId: "wrong-client-id",
        clientSecret: "wrong-client-secret"
    };
    SmtpConfiguration smtpConfig = {
        port: 3586,
        security: START_TLS_AUTO,
        auth: grantConfig
    };

    // OAuth2 panics for invalid credentials grant
    SmtpClient|error result = trap new ("127.0.0.1", "someone@localhost.com", clientConfig = smtpConfig);
    if result is SmtpClient {
        check stopOAuth2SmtpServer();
        test:assertFail("Expected an error when using invalid client credentials");
    }
}

@test:Config {
    groups: ["smtp", "oauth2"],
    dependsOn: [testSendEmailWithInvalidClientCredentials]
}
function testSendEmailWithInvalidDirectToken() returns error? {
    SmtpConfiguration smtpConfig = {
        port: 3586,
        security: START_TLS_AUTO,
        auth: "invalid_token"
    };
    SmtpClient smtpClient = check new ("127.0.0.1", "someone@localhost.com", clientConfig = smtpConfig);
    Error? sendResult = smtpClient->sendMessage({
        to: "hascode@localhost",
        subject: "Should fail",
        body: "This email should not be sent."
    });
    check stopOAuth2SmtpServer();
    if sendResult is () {
        test:assertFail("Expected an error when using an invalid token");
    }
}

@test:Config {
    groups: ["smtp", "oauth2"]
}
function testOAuth2InitRequiresUsername() {
    SmtpConfiguration smtpConfig = {
        port: 3586,
        security: START_TLS_AUTO,
        auth: "smtp_oauth2_test_token_12345"
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

public function validateOAuth2EmailCount(int expectedCount) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpOAuth2EmailSendTest"
} external;
