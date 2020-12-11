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

boolean onEmailMessageInvokedImap = false;
boolean onErrorInvokedImap = false;
string receivedMessageImap = "";
string receivedErrorImap = "";

function isOnEmailInvokedImap() returns boolean {
    int i = 0;
    while ((!onEmailMessageInvokedImap) && (i < 10)) {
    	 runtime:sleep(1000);
    	 i += 1;
    }
    return onEmailMessageInvokedImap;
}

function isonErrorInvokedImap() returns boolean {
    int i = 0;
    while ((!onErrorInvokedImap) && (i < 10)) {
         runtime:sleep(1000);
         i += 1;
    }
    return onErrorInvokedImap;
}

function getreceivedMessageImap() returns string {
    int i = 0;
    while ((!onEmailMessageInvokedImap) && (i < 10)) {
         runtime:sleep(1000);
         i += 1;
    }
    return <@untainted>receivedMessageImap;
}

function getreceivedErrorImap() returns string {
    int i = 0;
    while ((!onErrorInvokedImap) && (i < 10)) {
         runtime:sleep(1000);
         i += 1;
    }
    return <@untainted>receivedErrorImap;
}

@test:Config {
    dependsOn: ["testReceiveSimpleEmailImap"]
}
function testListenEmailImap() {

    Error? listenerStatus = startImapListener();
    if (listenerStatus is Error) {
        test:assertFail(msg = "Error while starting IMAP listener.");
    }

    ImapListener emailServer = new ({
                               host: "127.0.0.1",
                               username: "hascode",
                               password: "abcdef123",
                               pollingIntervalInMillis: 2000,
                               port: 3993,
                               enableSsl: true,
                               properties: ()
                           });

    service object {} emailObserver = service object {
        remote function onEmailMessage(Message emailMessage) {
            receivedMessageImap = <@untainted>emailMessage.subject;
            onEmailMessageInvokedImap = true;
        }

        remote function onError(Error emailError) {
            receivedErrorImap = <@untainted>emailError.message();
            onErrorInvokedImap = true;
        }
    };

    error? attachStatus = emailServer.attach(emailObserver, "");
    error? startStatus = emailServer.start();

    Error? emailSentStatus = sendEmailImapListener();
    if (emailSentStatus is Error) {
        test:assertFail(msg = "Error while sending email for IMAP listener.");
    }
    test:assertTrue(onEmailMessageInvokedImap, msg = "Email is not received with method, onEmailMessage with IMAP.");
    test:assertFalse(onErrorInvokedImap,
        msg = "An error occurred while listening and invoked method, onError with IMAP.");
    test:assertEquals(receivedMessageImap, "Test E-Mail", msg = "Listened email subject is not matched with IMAP.");

    listenerStatus = stopImapListener();
    if (listenerStatus is error) {
        test:assertFail(msg = "Error while stopping IMAP listener.");
    }

    test:assertTrue(onErrorInvokedImap, msg = "Error was not listened by method, onError with IMAP.");
    test:assertTrue(stringutils:contains(receivedErrorImap, "Couldn't connect to host, port: 127.0.0.1,"),
        msg = "Listened error message is not matched with IMAP.");

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
