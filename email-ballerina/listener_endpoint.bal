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
import ballerina/log;
import ballerina/task;

# Represents a service listener that monitors the email server location.
public class Listener {

    private ListenerConfig config;
    private task:JobId id;

    # Gets invoked during the `email:Listener` initialization.
    #
    # + ListenerConfig - Configurations for Email endpoint
    public isolated function init(ListenerConfig listenerConfig) {
        self.config = listenerConfig;
        checkpanic externalInit(self, self.config);
    }

    # Starts the `email:Listener`.
    # ```ballerina
    # email:Error? result = emailListener.start();
    # ```
    #
    # + return - () or else error upon failure to start the listener
    public isolated function 'start() returns error? {
        return self.internalStart();
    }

    # Stops the `email:Listener`.
    # ```ballerina
    # email:Error? result = emailListener.__stop();
    # ```
    #
    # + return - () or else error upon failure to stop the listener
    public isolated function __stop() returns error? {
        check self.stop();
    }

    # Binds a service to the `email:Listener`.
    # ```ballerina
    # email:Error? result = emailListener.attach(helloService, hello);
    # ```
    #
    # + s - Type descriptor of the service
    # + name - Name of the service
    # + return - `()` or else a `email:Error` upon failure to register the listener
    public isolated function attach(service object {} s, string[]|string? name = ()) returns error? {
        if(name is string?) {
            return self.register(s, name);
        }
    }

    # Stops consuming messages and detaches the service from the `email:Listener`.
    # ```ballerina
    # email:Error? result = emailListener.detach(helloService);
    # ```
    #
    # + s - Type descriptor of the service
    # + return - `()` or else a `email:Error` upon failure to detach the service
    public isolated function detach(service object {} s) returns error? {

    }

    # Stops the `email:Listener` forcefully.
    # ```ballerina
    # email:Error? result = emailListener.immediateStop();
    # ```
    #
    # + return - `()` or else a `email:Error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        check self.stop();
    }

    # Stops the `email:Listener` gracefully.
    # ```ballerina
    # email:Error? result = emailListener.gracefulStop();
    # ```
    #
    # + return - () or else error upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        check self.stop();
    }

    isolated function internalStart() returns error? {
        time:Utc currentUtc = time:utcNow();
        time:Utc newTime = time:utcAddSeconds(currentUtc, 0.1);
        time:Civil time = time:utcToCivil(newTime);
        task:JobId id = check task:scheduleJobRecurByFrequency(new Job(self), self.config.pollingInterval, startTime = time);
        log:printInfo("User " + self.config.username + " is listening to remote server at " + self.config.host + "...");
    }

    isolated function stop() returns error? {
        task:UnscheduleJob(id);
        log:printInfo("Stopped listening to remote server at " + self.config.host);
    }

    isolated function poll() returns error? {
        return poll(self);
    }

    # Registers for the Email service.
    # ```ballerina
    # emailListener.register(helloService, hello);
    # ```
    #
    # + emailService - Type descriptor of the service
    # + name - Service name
    public isolated function register(service object {} emailService, string? name) {
        register(self, emailService);
    }
}

Class Job {

    *task:Job;
    Listener l;

    public function execute() {
        var result = self.l.poll();
        if (result is error) {
            log:printError("Error while executing poll function", result);
        }
    }

    isolated function init(Listener l) {
        self.l = l;
    }
}

# Configuration for Email listener endpoint.
#
# + host - Email server host
# + username - Email server access username
# + password - Email server access password
# + protocol - Email server access protocol, "IMAP" or "POP"
# + protocolConfig - POP3 or IMAP4 protocol configuration
# + pollingInterval - Periodic time interval to check new update
public type ListenerConfig record {|
    string host;
    string username;
    string password;
    string protocol = "IMAP";
    PopConfig|ImapConfig? protocolConfig = ();
    decimal pollingInterval = 60000;
|};

isolated function poll(Listener listenerEndpoint) returns error? = @java:Method{
    name: "poll",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;

isolated function externalInit(Listener listenerEndpoint, ListenerConfig config) returns error? = @java:Method{
    name: "init",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;

isolated function register(Listener listenerEndpoint, service object {} emailService) = @java:Method{
    name: "register",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;
