// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/email;

configurable string senderAddress = ?;
configurable string receiverAddress = ?;
configurable string senderPassword = ?;

public function main() returns error? {

    // Creates an SMTP client to Gmail. The default port number `465` is
    // used over SSL with these configurations.
    email:SmtpClient smtpClient = check new ("smtp.gmail.com",
        senderAddress, senderPassword);

    // Defines the email that is required to be sent. `'from` address is
    // automatically set from the client credentials.
    email:Message emailMessage = {
        to: receiverAddress,
        subject: "Sample Email Title",

        // Body content (text) of the email is added as follows.
        body: "This is a sample email text body."
    };

    // Sends the email message with the client.
    check smtpClient->sendMessage(emailMessage);

}
