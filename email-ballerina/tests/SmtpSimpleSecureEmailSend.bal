// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/java;
import ballerina/stringutils;
import ballerina/test;

@test:Config {
}
function testSendSimpleEmail() {
    string host = "127.0.0.1";
    string username = "hascode";
    string password = "abcdef123";
    string toAddress = "hascode@localhost";
    string subject = "Test E-Mail";
    string body = "This is a test e-mail.";
    string fromAddress = "someone@localhost.com";

    error? serverStatus = startSimpleSecureSmtpServer();
    SmtpConfig smtpConfig = {
        port: 3465
    };

    SmtpClient|Error smtpClientOrError = new (host, username,  password, smtpConfig);
    if (smtpClientOrError is Error) {
        test:assertFail(msg = "Error while initializing the SMTP client.");
    }
    SmtpClient smtpClient = checkpanic smtpClientOrError;
    Message email = {
        to: toAddress,
        subject: subject,
        body: body,
        'from: fromAddress
    };
    Error? response = smtpClient->sendEmailMessage(email);
    if (response is Error) {
        test:assertFail(msg = "Error while sending an email.");
    }

    error? emailValidation = validateSimpleSecureEmail();

    if (emailValidation is error) {
        test:assertFail(msg = "Error while validating the received email.");
    }

    smtpClientOrError = new (host, username,  "wrongPassword", smtpConfig);
    if (smtpClientOrError is Error) {
        test:assertFail(msg = "Error while initializing the SMTP client.");
    }
    smtpClient = checkpanic smtpClientOrError;
    response = smtpClient->sendEmailMessage(email);
    if (response is Error) {
        test:assertTrue(stringutils:contains(response.message(), "Authentication credentials invalid"),
            msg = "Error while authentication failure.");
    } else {
        test:assertFail(msg = "No error returned when wrong SMTP password is given.");
    }
    serverStatus = stopSimpleSecureSmtpServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping secure SMTP server.");
    }
}

public function startSimpleSecureSmtpServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.SmtpSimpleSecureEmailSendTest"
} external;

public function stopSimpleSecureSmtpServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.SmtpSimpleSecureEmailSendTest"
} external;

public function validateSimpleSecureEmail() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.SmtpSimpleSecureEmailSendTest"
} external;
