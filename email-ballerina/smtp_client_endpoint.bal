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
    public isolated function init(string host, string username, string password, SmtpConfig clientConfig = {})
            returns Error? {
        return initSmtpClientEndpoint(self, host, username, password, clientConfig);
    }


    # Sends an email message.
    # ```ballerina
    # email:Error? response = smtpClient->sendMessage(email);
    # ```
    #
    # + email - An `email:Message` message, which needs to be sent to the recipient
    # + return - An `email:Error` if failed to send the message to the recipient or else `()`
    remote isolated function sendMessage(Message email) returns Error? {
        if (email?.contentType == ()) {
            email.contentType = "text/plain";
        } else if (!self.containsType(email?.contentType, "text")) {
            return error SendError("Content type of the email should be text.");
        }
        self.putAttachmentToArray(email);
        return send(self, email);
    }

    # Sends an email message with optional parameters.
    # ```ballerina
    # email:Error? response = smtpClient->send(toAddress, subject, fromAddress,
    #   emailBody, sender="eve@abc.com");
    # ```
    #
    # + to - TO address list
    # + subject - Subject of email
    # + from - From address
    # + options - Optional parameters of the email
    # + return - An `email:Error` if failed to send the message to the recipient or else `()`
    remote isolated function send(string|string[] to, string subject, string 'from, string body, *Options options)
            returns Error? {
        Message email = {
            to: to,
            subject: subject,
            'from: 'from,
            body: body
        };
        string? htmlBody = options?.htmlBody;
        if (!(htmlBody is ())) {
            email.htmlBody = <string>htmlBody;
        }
        string? contentType = options?.contentType;
        if (!(contentType is ())) {
            email.contentType = <string>contentType;
        }
        map<string>? headers = options?.headers;
        if (!(headers is ())) {
            email.headers = <map<string>>headers;
        }
        string|string[]? cc = options?.cc;
        if (!(cc is ())) {
            email.cc = <string|string[]>cc;
        }
        string|string[]? bcc = options?.bcc;
        if (!(bcc is ())) {
            email.bcc = <string|string[]>bcc;
        }
        string|string[]? replyTo = options?.replyTo;
        if (!(replyTo is ())) {
            email.replyTo = <string|string[]>replyTo;
        }
        string? sender = options?.sender;
        if (!(sender is ())) {
            email.sender = <string>sender;
        }
        mime:Entity|Attachment|(mime:Entity|Attachment)[]? attachments = options?.attachments;
        if (!(attachments is ())) {
            email.attachments = <mime:Entity|Attachment|(mime:Entity|Attachment)[]>attachments;
        }
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
        mime:Entity|Attachment|(mime:Entity|Attachment)[]|() attachments = email?.attachments;
        if (attachments is Attachment || attachments is mime:Entity) {
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
# + secureSocket - Secure socket configuration
public type SmtpConfig record {|
    int port = 465;
    Security security = SSL;
    SecureSocket secureSocket?;
|};
