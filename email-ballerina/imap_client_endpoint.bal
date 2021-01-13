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

# Represents an IMAP Client, which interacts with an IMAP Server.
public client class ImapClient {

    # Gets invoked during the `email:ImapClient` initialization.
    #
    # + host - Host of the IMAP Client
    # + username - Username of the IMAP Client
    # + password - Password of the IMAP Client
    # + clientConfig - Configurations for the IMAP Client
    # + return - An `email:Error` if failed while creating the client or else `()`
    public isolated function init(@untainted string host, @untainted string username, @untainted string password,
            ImapConfig clientConfig = {}) returns Error? {
        return initImapClientEndpoint(self, host, username, password, clientConfig);
    }

    # Reads a message.
    # ```ballerina
    # email:Message|email:Error emailResponse = imapClient->receiveEmailMessage();
    # ```
    #
    # + folder - Folder to read emails. The default value is `INBOX`
    # + return - An`email:Message` if reading the message is successful, `()` if there are no emails in the specified
    #            folder, or else an `email:Error` if the recipient failed to receive the message
    remote isolated function receiveEmailMessage(string folder = DEFAULT_FOLDER) returns Message|Error? {
        return imapRead(self, folder);
    }

}

isolated function initImapClientEndpoint(ImapClient clientEndpoint, string host, string username, string password,
        ImapConfig config) returns Error? = @java:Method {
    name : "initImapClientEndpoint",
    'class : "org.ballerinalang.stdlib.email.client.EmailAccessClient"
} external;

isolated function imapRead(ImapClient clientEndpoint, string folder) returns Message|Error? = @java:Method {
    name : "readMessage",
    'class : "org.ballerinalang.stdlib.email.client.EmailAccessClient"
} external;

# Configuration of the IMAP Endpoint.
#
# + port - Port number of the IMAP server
# + security - Type of security channel
# + properties - IMAP properties to override the existing configuration
public type ImapConfig record {|
    int port = 993;
    Security? security = ();
    map<string>? properties = ();
|};
