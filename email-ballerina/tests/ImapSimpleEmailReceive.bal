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

@test:Config {}
function testReceiveSimpleEmailImap() returns @tainted error? {
    string host = "127.0.0.1";
    string username = "hascode";
    string password = "abcdef123";

    Error? serverStatus = startSimpleImapServer();
    if (serverStatus is Error) {
        test:assertFail(msg = "Error while starting secure IMAP server.");
    }

    ImapConfiguration imapConfig = {
         port: 3143,
         security: START_TLS_NEVER
    };
    ImapClient|Error imapClientOrError = new (host, username, password, imapConfig);
    if (imapClientOrError is Error) {
        test:assertFail(msg = "Error while initializing the IMAP4 client.");
    }
    ImapClient imapClient = check imapClientOrError;
    Message|Error? email = imapClient->receiveMessage(timeout = 2);
    if (email is Error) {
        test:assertFail(msg = "Error while zero reading email in simple IMAP test.");
    } else if (email is Message) {
        test:assertFail(msg = "Non zero emails received in zero read IMAP test.");
    }
    Error? emailSendStatus = sendEmailSimpleImapServer();
    if (emailSendStatus is Error) {
        test:assertFail(msg = "Error while sending email to secure IMAP server.");
    }

    email = imapClient->receiveMessage();
    if (email is Error) {
        test:assertFail(msg = "Error while reading email in simple IMAP test.");
    } else if (email is ()) {
        test:assertFail(msg = "No emails were read in IMAP test.");
    } else {
        test:assertEquals(email.subject, "Test E-Mail", msg = "Email subject is not matched.");
    }

    Error? closeStatus = imapClient->close();
    if (closeStatus is Error) {
        test:assertFail(msg = "Error while closing secure POP server.");
    }

    serverStatus = stopSimpleImapServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping secure IMAP server.");
    }

}

public function startSimpleImapServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ImapSimpleEmailReceiveTest"
} external;

public function stopSimpleImapServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ImapSimpleEmailReceiveTest"
} external;

public function sendEmailSimpleImapServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ImapSimpleEmailReceiveTest"
} external;
