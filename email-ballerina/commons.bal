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

import ballerina/mime;

# Email message properties.
#
# + to - TO address list
# + subject - Subject of email
# + from - From address
# + body - Text typed body of the email message
# + htmlBody - HTML typed body of the email message
# + cc - CC address list
# + bcc - BCC address list
# + replyTo - Reply To addresses
# + contentType - Content Type of the Body
# + headers - Header list
# + sender - Sender's address
# + attachments - Email attachements
public type Message record {|
    string|string[] to;
    string subject;
    string 'from?;
    string body?;
    string htmlBody?;
    string|string[] cc?;
    string|string[] bcc?;
    string|string[] replyTo?;
    string contentType?;
    map<string> headers?;
    string sender?;
    mime:Entity|Attachment|(mime:Entity|Attachment)[] attachments?;
|};

# Optional parameters for an Email message.
#
# + htmlBody - HTML typed body of the email message
# + contentType - Content Type of the Body
# + headers - Header list
# + cc - CC address list
# + bcc - BCC address list
# + replyTo - Reply To addresses
# + sender - Sender's address
# + attachments - Email attachements
public type Options record {|
    string htmlBody?;
    string contentType?;
    map<string> headers?;
    string|string[] cc?;
    string|string[] bcc?;
    string|string[] replyTo?;
    string sender?;
    mime:Entity|Attachment|(mime:Entity|Attachment)[] attachments?;
|};

# Email attachment.
#
# + filePath - File path of the attachment
# + contentType - Content Type of the attachment
public type Attachment record {|
  string filePath;
  string contentType;
|};

# Secure Socket configuration.
#
# + cert - Server certificate path
# + protocol - SSL or TLS protocol
# + ciphers - Ciper used
# + verifyHostName - Enable hostname verification
public type SecureSocket record {|
    string cert;
    record {|
        Protocol name;
        string[] versions = [];
    |} protocol?;
    string[] ciphers?;
    boolean verifyHostName = true;
|};

# Security type.
#
# + START_TLS_AUTO - If STARTTLS exists use it else use plaintext
# + START_TLS_ALWAYS - Use STARTTLS if not available throw error
# + START_TLS_NEVER - Use plaintext
# + SSL - Use SSL
public enum Security {
  START_TLS_AUTO,
  START_TLS_ALWAYS,
  START_TLS_NEVER,
  SSL
}

# Represents protocol options.
public enum Protocol {
   TLS
}

# Default folder to read emails.
public const DEFAULT_FOLDER = "INBOX";
