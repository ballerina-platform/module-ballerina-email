// Copyright (c) 2025 WSO2 LLC. (http://www.wso2.org)
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

// The connection stays in plaintext, so the mailbox password crosses the network in the clear
public function neverStartTls() returns error? {
    email:SmtpClient _ = check new ("smtp.email.com", "sender@email.com", "pass123", clientConfig = {
        port: 25,
        security: email:START_TLS_NEVER
    });
}

// The same on a POP client, with the configuration held in a variable
email:PopConfiguration plaintextPopConfig = {
    port: 110,
    security: email:START_TLS_NEVER
};

public function neverStartTlsPop() returns error? {
    email:PopClient _ = check new ("pop.email.com", "reader@email.com", "pass123",
            clientConfig = plaintextPopConfig);
}

// Negative case - TLS required
public function alwaysStartTls() returns error? {
    email:SmtpClient _ = check new ("smtp.email.com", "sender@email.com", "pass123", clientConfig = {
        port: 587,
        security: email:START_TLS_ALWAYS
    });
}

// Negative case - the default of implicit TLS
public function defaultSecurity() returns error? {
    email:SmtpClient _ = check new ("smtp.email.com", "sender@email.com", "pass123", clientConfig = {
        port: 465
    });
}
