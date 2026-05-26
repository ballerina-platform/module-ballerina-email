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
import ballerina/oauth2;

# Represents an SMTP Client, which interacts with an SMTP Server.
public isolated client class SmtpClient {

    private final string? accessToken;
    private final oauth2:ClientOAuth2Provider? oauth2Provider;

    # Gets invoked during the `email:SmtpClient` initialization.
    #
    # + host - Host of the SMTP Client
    # + username - Username of the SMTP Client if authentication is required
    # + password - Password of the SMTP Client if basic authentication is used
    # + clientConfig - Configurations for SMTP Client
    # + return - An `email:Error` if failed to initialize or else `()`
    public isolated function init(string host, string? username = (), string? password = (),
            *SmtpConfiguration clientConfig) returns Error? {
        OAuth2GrantConfig|string|() auth = clientConfig.auth;
        if username is string && password is string && auth is () {
            self.accessToken = ();
            self.oauth2Provider = ();
            return initSmtpClientEndpoint(self, host, username, password, clientConfig);
        }
        if username is string && password is () && auth is string {
            self.accessToken = auth;
            self.oauth2Provider = ();
            return initOAuth2SmtpClientEndpoint(self, host, username, clientConfig);
        }
        if username is string && password is () && auth is OAuth2GrantConfig {
            self.accessToken = ();
            self.oauth2Provider = new oauth2:ClientOAuth2Provider(auth);
            return initOAuth2SmtpClientEndpoint(self, host, username, clientConfig);
        }
        if username is () && password is () && auth is () {
            self.accessToken = ();
            self.oauth2Provider = ();
            return initNoAuthSmtpClientEndpoint(self, host, clientConfig);
        }
        self.accessToken = ();
        self.oauth2Provider = ();
        return error Error("Invalid configuration: provide 'username'+'password' for basic auth, " +
                           "'username'+'auth' for OAuth2, or neither for unauthenticated mode.");
    }

    # Sends an email message.
    # ```ballerina
    # email:Error? response = smtpClient->sendMessage(email);
    # ```
    #
    # + email - An `email:Message` message, which needs to be sent to the recipient
    # + return - An `email:Error` if failed to send the message to the recipient or else `()`
    remote isolated function sendMessage(Message email) returns Error? {
        if email.contentType is string && !self.containsType(email?.contentType, "text") {
            return error Error("Content type of the email should be text.");
        }
        self.putAttachmentToArray(email);
        return self.dispatchSend(email);
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
    # + body - Text body of the email
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
        return self.dispatchSend(email);
    }

    private isolated function dispatchSend(Message email) returns Error? {
        string? token = self.accessToken;
        if token is string {
            return sendWithOAuth2(self, email, token);
        }
        oauth2:ClientOAuth2Provider? provider = self.oauth2Provider;
        if provider is oauth2:ClientOAuth2Provider {
            string|oauth2:Error generatedToken = provider.generateToken();
            if generatedToken is error {
                return error Error("Failed to generate OAuth2 token: " + generatedToken.message());
            }
            return sendWithOAuth2(self, email, generatedToken);
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
        SmtpConfiguration config) returns Error? = @java:Method {
    name: "initClientEndpoint",
    'class: "io.ballerina.stdlib.email.client.SmtpClient"
} external;

isolated function initOAuth2SmtpClientEndpoint(SmtpClient clientEndpoint, string host, string username,
        SmtpConfiguration config) returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.client.SmtpClient"
} external;

isolated function initNoAuthSmtpClientEndpoint(SmtpClient clientEndpoint, string host, SmtpConfiguration config)
returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.client.SmtpClient"
} external;

isolated function send(SmtpClient clientEndpoint, Message email) returns Error? = @java:Method {
    name: "sendMessage",
    'class: "io.ballerina.stdlib.email.client.SmtpClient"
} external;

isolated function sendWithOAuth2(SmtpClient clientEndpoint, Message email, string accessToken) returns Error? =
    @java:Method {
    name: "sendMessageWithOAuth2",
    'class: "io.ballerina.stdlib.email.client.SmtpClient"
} external;

# Configuration of the SMTP Endpoint.
#
# + port - Port number of the SMTP server
# + security - Type of security channel
# + secureSocket - Secure socket configuration
# + auth - Authentication configuration: a direct access token (`string`) for XOAUTH2, or an
#           `OAuth2GrantConfig` to fetch a token from an authorization server
public type SmtpConfiguration record {|
    int port = 465;
    Security security = SSL;
    SecureSocket secureSocket?;
    OAuth2GrantConfig|string auth?;
|};
