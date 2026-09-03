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

// Withdrawn TLS versions named alongside a current one
public function weakTlsVersions() returns error? {
    email:SmtpClient _ = check new ("smtp.email.com", "sender@email.com", "pass123", clientConfig = {
        port: 465,
        secureSocket: {
            cert: "path/to/certfile.crt",
            protocol: {
                name: email:TLS,
                versions: ["TLSv1.2", "TLSv1.1", "TLSv1.0"]
            }
        }
    });
}

// A broken SSL version on an IMAP client
public function sslVersion() returns error? {
    email:ImapClient _ = check new ("imap.email.com", "reader@email.com", "pass123", clientConfig = {
        port: 993,
        secureSocket: {
            cert: "path/to/certfile.crt",
            protocol: {
                name: email:TLS,
                versions: ["SSLv3"]
            }
        }
    });
}

// Negative case - current TLS versions only
public function currentTlsVersions() returns error? {
    email:SmtpClient _ = check new ("smtp.email.com", "sender@email.com", "pass123", clientConfig = {
        port: 465,
        secureSocket: {
            cert: "path/to/certfile.crt",
            protocol: {
                name: email:TLS,
                versions: ["TLSv1.2", "TLSv1.3"]
            }
        }
    });
}
