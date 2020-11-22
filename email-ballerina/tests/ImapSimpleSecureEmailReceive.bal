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
import ballerina/test;

@test:Config {}
function testReceiveSimpleEmailImap() {
    string host = "127.0.0.1";
    string username = "hascode";
    string password = "abcdef123";

    Error? serverStatus = startSimpleSecureImapServer();
    if (serverStatus is Error) {
        test:assertFail(msg = "Error while starting secure IMAP server.");
    }

    ImapConfig imapConfig = {
         port: 3993,
         enableSsl: true
    };
    ImapClient imapClient = new (host, username, password, imapConfig);
    Email|Error? email = imapClient->read();
    if (email is Error) {
        test:assertFail(msg = "Error while zero reading email in simple IMAP test.");
    } else if (email is Email) {
        test:assertFail(msg = "Non zero emails received in zero read IMAP test.");
    }
    Error? emailSendStatus = sendEmailSimpleSecureImapServer();
    if (emailSendStatus is Error) {
        test:assertFail(msg = "Error while sending email to secure IMAP server.");
    }

    email = imapClient->read();
    if (email is Error) {
        test:assertFail(msg = "Error while reading email in simple IMAP test.");
    } else if (email is ()) {
        test:assertFail(msg = "No emails were read in IMAP test.");
    } else {
        test:assertEquals(email.subject, "Test E-Mail", msg = "Email subject is not matched.");
    }

    serverStatus = stopSimpleSecureImapServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping secure IMAP server.");
    }

}

public function startSimpleSecureImapServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ImapSimpleSecureEmailReceiveTest"
} external;

public function stopSimpleSecureImapServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ImapSimpleSecureEmailReceiveTest"
} external;

public function sendEmailSimpleSecureImapServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ImapSimpleSecureEmailReceiveTest"
} external;
