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

@test:Config {
    groups: ["smtp", "no-auth"]
}
function testSendNoAuthEmail() returns error? {
    string host = "127.0.0.1";
    string fromAddress = "sender@localhost.com";
    string toAddress = "recipient@localhost.com";
    string subject = "Test E-Mail No Auth";
    string body = "This is a test e-mail sent with no-auth server.";

    error? serverStatus = startNoAuthSmtpServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while starting no-auth SMTP server.");
    }

    // Test with no username and password (no authentication)
    SmtpClient smtpClient = check new (host, port = 3025, security = START_TLS_NEVER);
    Message email = {
        'from: fromAddress,
        to: toAddress,
        subject: subject,
        body: body
    };

    Error? response = smtpClient->sendMessage(email);
    if (response is Error) {
        test:assertFail(msg = "Error while sending an email without authentication: " + response.message());
    }

    error? emailValidation = validateNoAuthEmail();
    if (emailValidation is error) {
        test:assertFail(msg = "Error while validating the received email: " + emailValidation.message());
    }

    serverStatus = stopNoAuthSmtpServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping no-auth SMTP server.");
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
