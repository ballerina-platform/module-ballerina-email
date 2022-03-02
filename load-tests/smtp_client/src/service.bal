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
import ballerina/log;
import ballerina/lang.runtime;
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
                    errorCount: resultCounter.retrieveErrorCount(),
                    receivedCount: resultCounter.retrieveReceivedCount()
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

configurable string EMAIL_SERVER_HOST = "email-service";

configurable int SMTP_PORT = 3025;
configurable string SMTP_CLIENT_USER = "user1";
configurable string SMTP_CLIENT_PASSWORD = "password1";

configurable int IMAP_PORT = 3143;
configurable string IMAP_CLIENT_USER = "user2";
configurable string IMAP_CLIENT_PASSWORD = "password2";

isolated function startPerfTest(decimal duration) returns error? {
    time:Utc startedTime = time:utcNow();
    time:Utc expiryTime = time:utcAddSeconds(startedTime, duration);
    email:SmtpClient smtpClient = check new (EMAIL_SERVER_HOST, SMTP_CLIENT_USER, SMTP_CLIENT_PASSWORD, port = SMTP_PORT, security = email:START_TLS_NEVER);
    email:Message message = {
        to: "user2",
        'from: "perf.test@localhost",
        subject: "Email Performance Test",
        body: "This is a test email which is initiated from performance test workflow for ballerina email module."
    };
    while time:utcDiffSeconds(expiryTime, time:utcNow()) > 0D {
        resultCounter.incrementSentCount();
        email:Error? result = smtpClient->sendMessage(message);
        if result is email:Error {
            log:printError("Error occurred while sending email", 'error = result);
            resultCounter.incrementErrorCount();
        }
    }
    email:Message completionEmail = {
        to: "user2",
        'from: "perf.test@localhost",
        subject: "Load Test Completed",
        body: "This is to inform that the load test is completed."
    };
    email:Error? result = smtpClient->sendMessage(completionEmail);
    if result is email:Error {
        log:printError("Error occurred while sending completion email", 'error = result);
    }
    return;
}

listener email:ImapListener imapListener = check new ({
    host: EMAIL_SERVER_HOST,
    port: IMAP_PORT,
    username: IMAP_CLIENT_USER,
    password: IMAP_CLIENT_PASSWORD,
    pollingInterval: 30,
    security: email:START_TLS_NEVER
});

isolated service on imapListener {
    isolated remote function onMessage(email:Message email) {
        if isLoadTestCompleted(email.subject) {
            lock {
                completed = true;
            }
            return;
        }
        resultCounter.incrementReceivedCount();
    }
}

isolated function isLoadTestCompleted(string subject) returns boolean {
    return "Load Test Completed" == subject;
}
