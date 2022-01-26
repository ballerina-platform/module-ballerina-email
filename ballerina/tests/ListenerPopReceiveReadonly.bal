// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/log;

boolean onReadonlyMessageInvokedPop = false;

function isOnReadonlyEmailInvokedPop() returns boolean {
    int i = 0;
    while ((!onReadonlyMessageInvokedPop) && (i < 10)) {
        runtime:sleep(1);
        i += 1;
    }
    return onReadonlyMessageInvokedPop;
}

@test:Config {
    dependsOn: [testListenEmailPop]
}
function testListenReadonlyEmailPop() returns error? {

    Error? listenerStatus = startPopListener();
    if (listenerStatus is Error) {
        test:assertFail(msg = "Error while starting POP listener for readonly listener.");
    }

    PopListener emailServer = check new ({
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

    Service emailObserver = service object {
        remote function onMessage(readonly & Message emailMessage) {
            log:printInfo("Listened a POP3 readonly message with subject: " + emailMessage.subject);
            onReadonlyMessageInvokedPop = true;
        }

        remote function onError(Error emailError) {
        }

        remote function onClose(Error? closeError) {
        }

    };

    _ = check emailServer.attach(emailObserver, "");
    _ = check emailServer.start();

    Error? emailSentStatus = sendEmailPopListener();
    if (emailSentStatus is Error) {
        test:assertFail(msg = "Error while sending email for POP readonly listener.");
    }

    test:assertTrue(isOnReadonlyEmailInvokedPop(), msg = "Email is not received with method, onMessage with POP.");
    listenerStatus = stopPopListener();
    if (listenerStatus is error) {
        test:assertFail(msg = "Error while stopping POP readonly listener.");
    }

    Error? closeStatus = emailServer.close();
    if (closeStatus is Error) {
        test:assertFail(msg = "Error while closing POP readonly listener.");
    }

}
