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
import ballerina/runtime;
import ballerina/stringutils;
import ballerina/test;

boolean onMessageInvokedPop = false;
boolean onErrorInvokedPop = false;
string receivedMessagePop = "";
string receivedErrorPop = "";

function isonMessageInvokedPop() returns boolean {
    int i = 0;
    while ((!onMessageInvokedPop) && (i < 10)) {
    	 runtime:sleep(1000);
    	 i += 1;
    }
    return onMessageInvokedPop;
}

function isonErrorInvokedPop() returns boolean {
    int i = 0;
    while ((!onErrorInvokedPop) && (i < 10)) {
         runtime:sleep(1000);
         i += 1;
    }
    return onErrorInvokedPop;
}

function getreceivedMessagePop() returns string {
    int i = 0;
    while ((!onMessageInvokedPop) && (i < 10)) {
         runtime:sleep(1000);
         i += 1;
    }
    return <@untainted>receivedMessagePop;
}

function getreceivedErrorPop() returns string {
    int i = 0;
    while ((!onErrorInvokedPop) && (i < 10)) {
         runtime:sleep(1000);
         i += 1;
    }
    return <@untainted>receivedErrorPop;
}

@test:Config {
    dependsOn: ["testReceiveSimpleEmailPop"]
}
function testListenEmailPop() {

    Error? listenerStatus = startPopListener();
    if (listenerStatus is Error) {
        test:assertFail(msg = "Error while starting POP listener.");
    }

    PopConfig popConfig = {
         port: 3995,
         enableSsl: true
    };
    Listener emailServer = new ({
                               host: "127.0.0.1",
                               username: "hascode",
                               password: "abcdef123",
                               protocol: "POP",
                               protocolConfig: popConfig,
                               pollingInterval: 2000
                           });

    service emailObserver = service {
        resource function onMessage(Email emailMessage) {
            receivedMessagePop = <@untainted>emailMessage.subject;
            onMessageInvokedPop = true;
        }

        resource function onError(Error emailError) {
            receivedErrorPop = <@untainted>emailError.message();
            onErrorInvokedPop = true;
        }
    };

    error? attachStatus = emailServer.__attach(emailObserver, "");
    error? startStatus = emailServer.__start();

    Error? emailSentStatus = sendEmailPopListener();
    if (emailSentStatus is Error) {
        test:assertFail(msg = "Error while sending email for POP listener.");
    }
    test:assertTrue(onMessageInvokedPop, msg = "Email is not received with method, onMessage with POP.");
    test:assertFalse(onErrorInvokedPop, msg = "An error occurred while listening and invoked method, onError with POP.");
    test:assertEquals(receivedMessagePop, "Test E-Mail", msg = "Listened email subject is not matched with POP.");

    listenerStatus = stopPopListener();
    if (listenerStatus is error) {
        test:assertFail(msg = "Error while stopping POP listener.");
    }

    test:assertTrue(onErrorInvokedPop, msg = "Error was not listened by method, onError with POP.");
    test:assertTrue(stringutils:contains(receivedErrorPop, "Couldn't connect to host, port: 127.0.0.1,"),
        msg = "Listened error message is not matched with POP.");

}

public function startPopListener() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/email/testutils/mockServerUtils/ListenerPopReceiveTest"
} external;

public function stopPopListener() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/email/testutils/mockServerUtils/ListenerPopReceiveTest"
} external;

public function sendEmailPopListener() returns Error? = @java:Method {
    'class: "org/ballerinalang/stdlib/email/testutils/mockServerUtils/ListenerPopReceiveTest"
} external;
