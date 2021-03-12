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
import ballerina/log;
import ballerina/task;

# Represents a service listener that monitors the email server location.
public class PopListener {

    private PopListenerConfiguration config;
    private task:JobId? jobId = ();

    # Gets invoked during the `email:PopListener` initialization.
    #
    # + ListenerConfig - Configurations for Email endpoint
    # + return - () or else error upon failure to initialize the listener
    public isolated function init(PopListenerConfiguration listenerConfig) returns Error? {
        self.config = listenerConfig;
        PopConfiguration popConfig = {
             port: listenerConfig.port,
             security: listenerConfig.security
        };
        SecureSocket? secureSocketParam = listenerConfig?.secureSocket;
        if (!(secureSocketParam is ())) {
            popConfig.secureSocket = secureSocketParam;
        }
        return externalInit(self, self.config, popConfig, "POP");
    }

    # Starts the `email:PopListener`.
    # ```ballerina
    # email:Error? result = emailListener.start();
    # ```
    #
    # + return - () or else error upon failure to start the listener
    public isolated function 'start() returns @tainted error? {
        return self.internalStart();
    }

    # Binds a service to the `email:PopListener`.
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

    # Stops consuming messages and detaches the service from the `email:PopListener`.
    # ```ballerina
    # email:Error? result = emailListener.detach(helloService);
    # ```
    #
    # + s - Type descriptor of the service
    # + return - `()` or else a `email:Error` upon failure to detach the service
    public isolated function detach(service object {} s) returns error? {

    }

    # Stops the `email:PopListener` forcefully.
    # ```ballerina
    # email:Error? result = emailListener.immediateStop();
    # ```
    #
    # + return - `()` or else a `email:Error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        check self.stop();
    }

    # Stops the `email:PopListener` gracefully.
    # ```ballerina
    # email:Error? result = emailListener.gracefulStop();
    # ```
    #
    # + return - () or else error upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        check self.stop();
    }

    isolated function internalStart() returns @tainted error? {
        self.jobId = check task:scheduleJobRecurByFrequency(new PopJob(self), self.config.pollingInterval);
        log:printInfo("User " + self.config.username + " is listening to remote server at " + self.config.host + "...");
    }

    isolated function stop() returns error? {
        var id = self.jobId;
        if (id is task:JobId) {
            check task:unscheduleJob(id);
            log:printInfo("Stopped listening to remote server at " + self.config.host);
        }
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

    # Close the POP server connection.
    # ```ballerina
    # email:Error? closeResult = emailClient->close();
    # ```
    #
    # + return - A `email:Error` if it can't close the connection or else `()`
    isolated function close() returns Error? {
        error? stopResult = self.stop();
        return externListenerClose(self);
    }

}

class PopJob {

    *task:Job;
    private PopListener popListener;

    public function execute() {
        var result = self.popListener.poll();
        if (result is error) {
            log:printError("Error while executing poll function", 'error = result);
        }
    }

    public isolated function init(PopListener popListener) {
        self.popListener = popListener;
    }
}

# Configuration for Email listener endpoint.
#
# + host - Email server host
# + username - Email server access username
# + password - Email server access password
# + pollingInterval - Periodic time interval (in seconds) to check new update
# + port - Port number of the POP server
# + security - Type of security channel
# + secureSocket - Secure socket configuration
public type PopListenerConfiguration record {|
    string host;
    string username;
    string password;
    decimal pollingInterval = 60;
    int port = 995;
    Security security = SSL;
    SecureSocket secureSocket?;
|};

isolated function externListenerClose(PopListener|ImapListener listenerEndpoint) returns error? = @java:Method{
    name: "close",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;

isolated function poll(PopListener|ImapListener listenerEndpoint) returns error? = @java:Method{
    name: "poll",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;

isolated function externalInit(PopListener|ImapListener listenerEndpoint,
    PopListenerConfiguration|ImapListenerConfiguration config, PopConfiguration|ImapConfiguration protocolConfig,
    string protocol) returns error? = @java:Method{
    name: "init",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;

isolated function register(PopListener|ImapListener listenerEndpoint, service object {} emailService) = @java:Method{
    name: "register",
    'class: "org.ballerinalang.stdlib.email.server.EmailListenerHelper"
} external;
