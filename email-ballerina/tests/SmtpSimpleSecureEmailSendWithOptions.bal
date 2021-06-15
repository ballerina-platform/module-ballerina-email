// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
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
    dependsOn: [testSendSimpleEmail]
}
function testSendSimpleEmailWithOptions() returns error? {
    string host = "127.0.0.1";
    string username = "someone@localhost.com";
    string password = "abcdef123";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail";
    string body = "This is a test e-mail.";
    string fromAddress = "someone@localhost.com";

    error? serverStatus = startSimpleSecureSmtpServerWithOptions();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while starting send-secure-with-options SMTP server.");
    }

    SmtpConfiguration smtpConfig = {
        port: 3465,
        secureSocket: {
            cert: "tests/resources/certsandkeys/greenmail.crt",
            protocol: {
                name: TLS,
                versions: ["TLSv1.2", "TLSv1.1"]
            },
            ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
            verifyHostName: false
        },
        security: START_TLS_ALWAYS
    };

    SmtpClient|Error smtpClientOrError = new (host, username,  password, smtpConfig);
    if (smtpClientOrError is Error) {
        test:assertFail(msg = "Error while initializing the SMTP client with options.");
    }
    SmtpClient smtpClient = check smtpClientOrError;

    Error? response = smtpClient->send(toAddress, subject, fromAddress, body);
    if (response is Error) {
        test:assertFail(msg = "Error while sending an send-secure-with-options email.");
    }
    Error? emailValidation = validateSimpleSecureEmailWithOptions();

    if (emailValidation is Error) {
        test:assertFail(msg = "Error while validating the received email.");
    }
    serverStatus = stopSimpleSecureSmtpServerWithOptions();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping send-secure-with-options SMTP server.");
    }

}

public function startSimpleSecureSmtpServerWithOptions() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.SmtpSimpleSecureEmailSendWithOptionsTest"
} external;

public function stopSimpleSecureSmtpServerWithOptions() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.SmtpSimpleSecureEmailSendWithOptionsTest"
} external;

public function validateSimpleSecureEmailWithOptions() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.SmtpSimpleSecureEmailSendWithOptionsTest"
} external;
