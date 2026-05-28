// Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
//
// WSO2 LLC. licenses this file to you under the Apache License,
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
configurable string tokenUrl = ?;
configurable string clientId = ?;
configurable string clientSecret = ?;

public function main() returns error? {

    // Configures the OAuth2 Client Credentials Grant. The token is fetched
    // at SmtpClient initialization and refreshed automatically on expiry.
    email:OAuth2ClientCredentialsGrantConfig grantConfig = {
        tokenUrl: tokenUrl,
        clientId: clientId,
        clientSecret: clientSecret
    };

    email:SmtpConfiguration smtpConfig = {
        port: 587,
        security: email:START_TLS_ALWAYS,
        auth: grantConfig
    };

    // The token endpoint is contacted here. If credentials are invalid,
    // this call fails immediately.
    email:SmtpClient smtpClient = check new ("smtp.example.com", senderAddress,
            clientConfig = smtpConfig);

    email:Message emailMessage = {
        to: receiverAddress,
        subject: "Sample Email via OAuth2",
        body: "This email was sent using the XOAUTH2 SASL mechanism."
    };

    check smtpClient->sendMessage(emailMessage);
}
