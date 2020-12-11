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
# + body - Body of the email message
# + cc - CC address list
# + bcc - BCC address list
# + replyTo - Reply To addresses
# + contentType - Content Type of the Body
# + headers - Header list
# + sender - Sender's address
# + attachments - Email attachements
public type Message record {|
    string[] to;
    string subject;
    string 'from;
    string body;
    string[] cc?;
    string[] bcc?;
    string[] replyTo?;
    string contentType?;
    map<string> headers?;
    string sender?;
    mime:Entity[] attachments?;
|};

# Default folder to read emails.
public const DEFAULT_FOLDER = "INBOX";
