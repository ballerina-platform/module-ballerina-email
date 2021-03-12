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

import ballerina/jballerina.java;
import ballerina/test;

@test:Config {
}
function testReceiveSimpleEmailPop() returns @tainted error? {
    string host = "127.0.0.1";
    string username = "hascode";
    string password = "abcdef123";

    Error? serverStatus = startSimpleSecurePopServer();
    if (serverStatus is Error) {
        test:assertFail(msg = "Error while starting secure POP server.");
    }

    PopConfiguration popConfig = {
         port: 3995,
         secureSocket: {
             cert: "tests/resources/certsandkeys/greenmail.crt",
             protocol: {
                 name: TLS,
                 versions: ["TLSv1.2", "TLSv1.1"]
             },
             ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
             verifyHostName: false
         }
    };
    PopClient|Error popClientOrError = new (host, username, password, popConfig);
    if (popClientOrError is Error) {
        test:assertFail(msg = "Error while initializing the POP3 client.");
    }
    PopClient popClient = check popClientOrError;
    Message|Error? email = popClient->receiveMessage(timeout = 2);
    if (email is Error) {
        test:assertFail(msg = "Error while zero reading email in simple POP test.");
    } else if (email is Message) {
        test:assertFail(msg = "Non zero emails received in zero read POP test.");
    }
    Error? emailSendStatus = sendEmailSimpleSecurePopServer();
    if (emailSendStatus is Error) {
        test:assertFail(msg = "Error while sending email to secure POP server.");
    }

    email = popClient->receiveMessage();
    if (email is Error) {
        test:assertFail(msg = "Error while reading email in simple POP test.");
    } else if (email is ()) {
        test:assertFail(msg = "No emails were read in POP test.");
    } else {
        test:assertEquals(email.subject, "Test E-Mail", msg = "Email subject is not matched.");
    }

    Error? closeStatus = popClient->close();
    if (closeStatus is Error) {
        test:assertFail(msg = "Error while closing secure POP server.");
    }

    serverStatus = stopSimpleSecurePopServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping secure POP server.");
    }

}

public function startSimpleSecurePopServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.PopSimpleSecureEmailReceiveTest"
} external;

public function stopSimpleSecurePopServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.PopSimpleSecureEmailReceiveTest"
} external;

public function sendEmailSimpleSecurePopServer() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.PopSimpleSecureEmailReceiveTest"
} external;
