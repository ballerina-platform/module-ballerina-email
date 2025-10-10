// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.com).
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

@test:BeforeGroups {
    value: ["no_auth"]
}
function beforeNoAuthEmailTests() returns error? {
    check startNoAuthSmtpServer();
}

@test:AfterGroups {
    value: ["no_auth"]
}
function afterNoAuthEmailTests() returns error? {
    check stopNoAuthSmtpServer();
}

@test:Config {
    groups: ["smtp", "no_auth"]
}
function testSendSimpleEmailWithoutAuth() returns error? {
    string host = "127.0.0.1";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail Without Auth";
    string body = "This is a test e-mail sent without authentication.";
    string fromAddress = "someone@localhost.com";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    SmtpClient smtpClient = check new (host, clientConfig = smtpConfig);

    Message email = {
        to: toAddress,
        subject,
        body,
        'from: fromAddress
    };

    check smtpClient->sendMessage(email);
    check validateNoAuthEmail();
}

@test:Config {
    groups: ["smtp", "no_auth"],
    dependsOn: [testSendSimpleEmailWithoutAuth]
}
function testSendEmailWithoutAuthUsingRemoteFunction() returns error? {
    string host = "127.0.0.1";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail Without Auth - Remote";
    string body = "This is a test e-mail sent using remote send function without authentication.";
    string fromAddress = "someone@localhost.com";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };
    SmtpClient smtpClient = check new (host, clientConfig = smtpConfig);
    check smtpClient->send(toAddress, subject, fromAddress, body);
}

@test:Config {
    groups: ["smtp", "no_auth"],
    dependsOn: [testSendEmailWithoutAuthUsingRemoteFunction]
}
function testSendComplexEmailWithoutAuth() returns error? {
    string host = "127.0.0.1";
    string subject = "Test Complex E-Mail Without Auth";
    string body = "This is a test e-mail.";
    string htmlBody = "<h1>This message is embedded in HTML tags.</h1>";
    string contentType = "text/html";
    string fromAddress = "someone1@localhost.com";
    string sender = "someone2@localhost.com";
    string[] toAddresses = ["hascode1@localhost", "hascode2@localhost"];
    string[] ccAddresses = ["hascode3@localhost", "hascode4@localhost"];
    string[] bccAddresses = ["hascode5@localhost", "hascode6@localhost"];
    string[] replyToAddresses = ["reply1@abc.com", "reply2@abc.com"];

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    SmtpClient smtpClient = check new (host, clientConfig = smtpConfig);

    Message email = {
        to: toAddresses,
        cc: ccAddresses,
        bcc: bccAddresses,
        subject,
        body,
        htmlBody: htmlBody,
        contentType,
        headers: {header1_name: "header1_value"},
        'from: fromAddress,
        sender,
        replyTo: replyToAddresses
    };

    check smtpClient->sendMessage(email);
}

@test:Config {
    groups: ["smtp", "no_auth"],
    dependsOn: [testSendComplexEmailWithoutAuth]
}
function testSendEmailWithoutAuthWithOptions() returns error? {
    string host = "127.0.0.1";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail Without Auth - With Options";
    string body = "This is a test e-mail.";
    string fromAddress = "someone@localhost.com";
    string ccAddress = "cc@localhost";
    string bccAddress = "bcc@localhost";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    SmtpClient smtpClient = check new (host, clientConfig = smtpConfig);

    check smtpClient->send(toAddress, subject, fromAddress, body,
        cc = ccAddress,
        bcc = bccAddress,
        sender = "sender@localhost.com"
    );
}

@test:Config {}
function testMismatchedUsernamePasswordError() returns error? {
    string host = "127.0.0.1";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    string expectedErrorMessage = "Mismatched input: 'username' and 'password' must either both be set or both be ().";

    // Test with only username provided
    SmtpClient|Error result1 = new (host, username = "user@localhost.com", clientConfig = smtpConfig);
    if result1 is SmtpClient {
        test:assertFail("Expected error when only username is provided without password");
    }
    test:assertEquals(result1.message(), expectedErrorMessage);

    // Test with only password provided
    SmtpClient|Error result2 = new (host, password = "password123", clientConfig = smtpConfig);
    if result2 is SmtpClient {
        test:assertFail("Expected error when only password is provided without username");
    }
    test:assertEquals(result2.message(), expectedErrorMessage);
}

@test:Config {}
function testExplicitNullAuthParameters() returns error? {
    string host = "127.0.0.1";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    // Test with explicitly passing nil for both username and password
    SmtpClient _ = check new (host, username = (), password = (), clientConfig = smtpConfig);
}

@test:Config {
    groups: ["smtp", "no_auth"]
}
function testNoAuthWithMissingFromField() returns error? {
    string host = "127.0.0.1";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail Without From";
    string body = "This email is missing the from field.";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    SmtpClient smtpClient = check new (host, clientConfig = smtpConfig);

    // Create message without 'from field
    Message email = {
        to: toAddress,
        subject,
        body
        // 'from field is intentionally missing
    };

    // This should fail with a specific error message
    Error? response = smtpClient->sendMessage(email);
    if response is Error {
        test:assertTrue(response.message().includes("'from' field is mandatory"),
            msg = "Expected error message about mandatory 'from' field");
    } else {
        test:assertFail("Expected error when 'from' field is missing in no-auth mode");
    }
}

@test:Config {
    groups: ["smtp", "no_auth"]
}
function testNoAuthWithEmptyFromField() returns error? {
    string host = "127.0.0.1";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail With Empty From";
    string body = "This email has an empty from field.";

    SmtpConfiguration smtpConfig = {
        port: 3026,
        security: START_TLS_AUTO
    };

    SmtpClient smtpClient = check new (host, clientConfig = smtpConfig);

    // Create message with empty 'from field
    Message email = {
        to: toAddress,
        subject,
        body,
        'from: "" // Empty from field
    };

    // This should fail with a specific error message
    Error? response = smtpClient->sendMessage(email);
    if response is Error {
        test:assertTrue(response.message().includes("'from' field is mandatory"),
            msg = "Expected error message about mandatory 'from' field");
    } else {
        test:assertFail("Expected error when 'from' field is empty in no-auth mode");
    }
}

public function startNoAuthSmtpServer() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpNoAuthEmailSendTest"
} external;

public function stopNoAuthSmtpServer() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpNoAuthEmailSendTest"
} external;

public function validateNoAuthEmail() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpNoAuthEmailSendTest"
} external;
