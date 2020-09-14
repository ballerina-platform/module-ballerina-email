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

@test:Config {
}
function testReceiveSimpleEmailPop() {
    string host = "127.0.0.1";
    string username = "hascode";
    string password = "abcdef123";

    Error? serverStatus = startSimpleSecurePopServer();
    if (serverStatus is Error) {
        test:assertFail(msg = "Error while starting secure POP server.");
    }

    PopConfig popConfig = {
         port: 3995,
         enableSsl: true
    };
    PopClient popClient = new (host, username, password, popConfig);
    Email|Error? email = popClient->read();
    if (email is Error) {
        test:assertFail(msg = "Error while zero reading email in simple POP test.");
    } else if (email is Email) {
        test:assertFail(msg = "Non zero emails received in zero read POP test.");
    }
    Error? emailSendStatus = sendEmailSimpleSecurePopServer();
    if (emailSendStatus is Error) {
        test:assertFail(msg = "Error while sending email to secure POP server.");
    }

    email = popClient->read();
    if (email is Error) {
        test:assertFail(msg = "Error while reading email in simple POP test.");
    } else if (email is ()) {
        test:assertFail(msg = "No emails were read in POP test.");
    } else {
        test:assertEquals(email.subject, "Test E-Mail", msg = "Email subject is not matched.");
    }

    serverStatus = stopSimpleSecurePopServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping secure POP server.");
    }

}

public function startSimpleSecurePopServer() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/email/testutils/mockServerUtils/PopSimpleSecureEmailReceiveTest"
} external;

public function stopSimpleSecurePopServer() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/email/testutils/mockServerUtils/PopSimpleSecureEmailReceiveTest"
} external;

public function sendEmailSimpleSecurePopServer() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/email/testutils/mockServerUtils/PopSimpleSecureEmailReceiveTest"
} external;
