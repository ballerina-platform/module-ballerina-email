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

boolean onMessageInvokedImap = false;
boolean onErrorInvokedImap = false;
boolean onCloseInvokedImap = false;
string receivedMessageImap = "";
string receivedErrorImap = "";
string receivedCloseImap = "";

function isOnEmailInvokedImap() returns boolean {
    int i = 0;
    while ((!onMessageInvokedImap) && (i < 10)) {
    	 runtime:sleep(1);
    	 i += 1;
    }
    return onMessageInvokedImap;
}

function isOnErrorInvokedImap() returns boolean {
    int i = 0;
    while ((!onErrorInvokedImap) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return onErrorInvokedImap;
}

function isOnCloseInvokedImap() returns boolean {
    int i = 0;
    while ((!onCloseInvokedImap) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return onCloseInvokedImap;
}

function getReceivedMessageImap() returns string {
    int i = 0;
    while ((!onMessageInvokedImap) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return receivedMessageImap;
}

function getReceivedErrorImap() returns string {
    int i = 0;
    while ((!onErrorInvokedImap) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return receivedErrorImap;
}

function getReceivedCloseImap() returns string {
    int i = 0;
    while ((!onCloseInvokedImap) && (i < 10)) {
         runtime:sleep(1);
         i += 1;
    }
    return receivedCloseImap;
}

@test:Config {
    dependsOn: [testReceiveSimpleEmailImap]
}
function testListenEmailImap() returns @tainted error? {

    Error? listenerStatus = startImapListener();
    if (listenerStatus is Error) {
        test:assertFail(msg = "Error while starting IMAP listener.");
    }

    ImapListener|Error emailServerOrError = new ({
                               host: "127.0.0.1",
                               username: "hascode",
                               password: "abcdef123",
                               pollingInterval: 2,
                               port: 3993,
                               secureSocket: {
                                    cert: {
                                        path: "tests/resources/certsandkeys/greenmail.crt"
                                    },
                                    protocol: {
                                        name: TLS,
                                        versions: ["TLSv1.2", "TLSv1.1"]
                                    },
                                    ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
                                    verifyHostname: false
                               }
                           });
    if (emailServerOrError is Error) {
        test:assertFail(msg = "Error while initializing the IMAP4 listener.");
    }
    ImapListener emailServer = check emailServerOrError;

    service object {} emailObserver = service object {
        remote function onMessage(Message emailMessage) {
            receivedMessageImap = emailMessage.subject;
            onMessageInvokedImap = true;
        }

        remote function onError(Error emailError) {
            receivedErrorImap = emailError.message();
            onErrorInvokedImap = true;
        }

        remote function onClose(Error? closeError) {
            if (closeError is Error) {
                receivedCloseImap = closeError.message();
            }
            onCloseInvokedImap = true;
        }

    };

    error? attachStatus = emailServer.attach(emailObserver, "");
    error? startStatus = emailServer.start();

    Error? emailSentStatus = sendEmailImapListener();
    if (emailSentStatus is Error) {
        test:assertFail(msg = "Error while sending email for IMAP listener.");
    }
    test:assertTrue(isOnEmailInvokedImap(), msg = "Email is not received with method, onMessage with IMAP.");
    test:assertFalse(isOnErrorInvokedImap(),
        msg = "An error occurred while listening and invoked method, onError with IMAP.");
    test:assertEquals(getReceivedMessageImap(), "Test E-Mail",
        msg = "Listened email subject is not matched with IMAP.");

    listenerStatus = stopImapListener();
    if (listenerStatus is error) {
        test:assertFail(msg = "Error while stopping IMAP listener.");
    }

    test:assertTrue(isOnErrorInvokedImap(), msg = "Error was not listened by method, onError with IMAP.");
    test:assertTrue(strings:includes(getReceivedErrorImap(), "connection failure"),
        msg = "Listened error message is not matched with IMAP.");

    Error? closeStatus = emailServer.close();
    if (closeStatus is Error) {
        test:assertFail(msg = "Error while closing IMAP listener.");
    }

    test:assertTrue(isOnCloseInvokedImap(), msg = "Close event was not listened by method, onClose with IMAP.");
    test:assertTrue(getReceivedCloseImap() == "",
        msg = "Error occurred while getting the error while closing the connection with IMAP.");

}

public function startImapListener() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ListenerImapReceiveTest"
} external;

public function stopImapListener() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ListenerImapReceiveTest"
} external;

public function sendEmailImapListener() returns Error? = @java:Method {
    'class: "org.ballerinalang.stdlib.email.testutils.ListenerImapReceiveTest"
} external;
