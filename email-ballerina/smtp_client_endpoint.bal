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
import ballerina/mime;

# Represents an SMTP Client, which interacts with an SMTP Server.
public client class SmtpClient {

    # Gets invoked during the `email:SmtpClient` initialization.
    #
    # + host - Host of the SMTP Client
    # + username - Username of the SMTP Client
    # + password - Password of the SMTP Client
    # + clientConfig - Configurations for SMTP Client
    # + return - An `email:Error` if failed to initialize or else `()`
    public isolated function init(@untainted string host, @untainted string username, @untainted string password,
            SmtpConfig clientConfig = {}) returns Error? {
        return initSmtpClientEndpoint(self, host, username, password, clientConfig);
    }

    # Sends a message.
    # ```ballerina
    # email:Error? response = smtpClient->sendEmailMessage(email);
    # ```
    #
    # + email - An `email:Message` message, which needs to be sent to the recipient
    # + return - An `email:Error` if failed to send the message to the recipient or else `()`
    remote isolated function sendEmailMessage(Message email) returns Error? {
        var body = email.body;
        if (email?.contentType == ()) {
            email.contentType = "text/plain";
        } else if (!self.containsType(email?.contentType, "text")) {
            return SendError("Content type of the email should be text.");
        }
        self.putAttachmentToArray(email);
        return send(self, email);
    }

    private isolated function containsType(string? contentType, string typeString) returns boolean {
        if (contentType is string) {
            string canonicalizedCtype = contentType.toLowerAscii();
            int? stringIndex = canonicalizedCtype.indexOf(typeString);
            return stringIndex is int;
        }
        return false;
    }

    private isolated function putAttachmentToArray(Message email) {
        Attachment|(mime:Entity|Attachment)[]|() attachments = email?.attachments;
        if (attachments is Attachment) {
            email.attachments = [attachments];
        }
    }

}

isolated function initSmtpClientEndpoint(SmtpClient clientEndpoint, string host, string username, string password,
        SmtpConfig config) returns Error? = @java:Method {
    name : "initClientEndpoint",
    'class : "org.ballerinalang.stdlib.email.client.SmtpClient"
} external;

isolated function send(SmtpClient clientEndpoint, Message email) returns Error? = @java:Method {
    name : "sendMessage",
    'class : "org.ballerinalang.stdlib.email.client.SmtpClient"
} external;

# Configuration of the SMTP Endpoint.
#
# + port - Port number of the SMTP server
# + security - Type of security channel
# + properties - SMTP properties to override the existing configuration
public type SmtpConfig record {|
    int port = 465;
    Security? security = ();
    map<string>? properties = ();
|};
