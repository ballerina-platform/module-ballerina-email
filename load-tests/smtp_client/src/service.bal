// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/time;
import ballerina/email;
import ballerina/http;

isolated boolean completed = false;
final Counter resultCounter = new;

service /perf\-test on new http:Listener(9090) {
    resource function get 'start(decimal duration) returns http:Accepted {
        _ = start startPerfTest(duration);
        return {};
    }

    resource function get status() returns http:Ok {
        boolean loadTestCompleted = false;
        lock {
            loadTestCompleted = completed;
        }
        if loadTestCompleted {
            return {
                body: {
                    completed: true,
                    sentCount: resultCounter.retrieveSentCount(),
                    errorCount: resultCounter.retrieveErrorCount()
                }
            };
        }
        return {
            body: {
                completed: false
            }
        };
    }
}

configurable string HOST = "client-svc.default.svc.cluster.local";
configurable int PORT = 3025;
configurable string USERNAME = "hascode";
configurable string PASSWORD = "abcdef123";

isolated function startPerfTest(decimal duration) returns error? {
    time:Utc startedTime = time:utcNow();
    time:Utc expiryTime = time:utcAddSeconds(startedTime, duration);
    email:SmtpClient smtpClient = check new (HOST, USERNAME, PASSWORD, port = PORT, security = email:START_TLS_NEVER);
    email:Message message = {
        to: "someone@localhost",
        'from: "perf.test@localhost",
        subject: "Email Performance Test",
        body: "This is a test email which is initiated from performance test workflow for ballerina email module."
    };
    while time:utcDiffSeconds(expiryTime, time:utcNow()) > 0D {
        resultCounter.incrementSentCount();
        email:Error? result = smtpClient->sendMessage(message);
        if result is email:Error {
            resultCounter.incrementErrorCount();
        }
    }
    lock {
        completed = true;
    }
    return;
}
