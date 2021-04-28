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
import ballerina/lang.runtime as runtime;
import ballerina/lang.'string as strings;
import ballerina/test;

boolean onMessageInvokedPop = false;
boolean onErrorInvokedPop = false;
boolean onCloseInvokedPop = false;
string receivedMessagePop = "";
string receivedErrorPop = "";
string receivedClosePop = "";

function isOnEmailInvokedPop() returns boolean {
    int i = 0;
    while ((!onMessageInvokedPop) && (i < 10)) {
    	 runtime:sleep(1);
    	 i += 1;
    }
    return onMessageInvokedPop;
}

function isOnErrorInvokedPop() returns boolean {
    int i = 0;
    while ((!onErrorInvokedPop) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return onErrorInvokedPop;
}

function isOnCloseInvokedPop() returns boolean {
    int i = 0;
    while ((!onCloseInvokedPop) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return onCloseInvokedPop;
}

function getReceivedMessagePop() returns string {
    int i = 0;
    while ((!onMessageInvokedPop) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return receivedMessagePop;
}

function getReceivedErrorPop() returns string {
    int i = 0;
    while ((!onErrorInvokedPop) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return receivedErrorPop;
}

function getReceivedClosePop() returns string {
    int i = 0;
    while ((!onCloseInvokedPop) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return receivedClosePop;
}

@test:Config {
    dependsOn: [testReceiveSimpleSecureEmailPop, testListenEmailImap]
}
function testListenEmailPop() returns @tainted error? {

    Error? listenerStatus = startPopListener();
    if (listenerStatus is Error) {
        test:assertFail(msg = "Error while starting POP listener.");
    }

    PopListener|Error emailServerOrError = new ({
                               host: "127.0.0.1",
                               username: "hascode",
                               password: "abcdef123",
                               pollingInterval: 2,
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
                           });
    if (emailServerOrError is Error) {
        test:assertFail(msg = "Error while initializing the POP3 listener.");
    }
    PopListener emailServer = check emailServerOrError;

    service object {} emailObserver = service object {
        remote function onMessage(Message emailMessage) {
            receivedMessagePop = emailMessage.subject;
            onMessageInvokedPop = true;
        }

        remote function onError(Error emailError) {
            receivedErrorPop = emailError.message();
            onErrorInvokedPop = true;
        }

        remote function onClose(Error? closeError) {
            if (closeError is Error) {
                receivedClosePop = closeError.message();
            }
            onCloseInvokedPop = true;
        }

    };

    error? attachStatus = emailServer.attach(emailObserver, "");
    error? startStatus = emailServer.start();

    Error? emailSentStatus = sendEmailPopListener();
    if (emailSentStatus is Error) {
        test:assertFail(msg = "Error while sending email for POP listener.");
    }

    test:assertTrue(isOnEmailInvokedPop(), msg = "Email is not received with method, onMessage with POP.");
    test:assertFalse(isOnErrorInvokedPop(),
        msg = "An error occurred while listening and invoked method, onError with POP.");
    test:assertEquals(getReceivedMessagePop(), "Test E-Mail", msg = "Listened email subject is not matched with POP.");

    listenerStatus = stopPopListener();
    if (listenerStatus is error) {
        test:assertFail(msg = "Error while stopping POP listener.");
    }

    test:assertTrue(isOnErrorInvokedPop(), msg = "Error was not listened by method, onError with POP.");
    test:assertTrue(strings:includes(getReceivedErrorPop(), "Open failed"),
        msg = "Listened error message is not matched with POP.");

    Error? closeStatus = emailServer.close();
    if (closeStatus is Error) {
        test:assertFail(msg = "Error while closing POP listener.");
    }

}

public function startPopListener() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ListenerPopReceiveTest"
} external;

public function stopPopListener() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ListenerPopReceiveTest"
} external;

public function sendEmailPopListener() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ListenerPopReceiveTest"
} external;
