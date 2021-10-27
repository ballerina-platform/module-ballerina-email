// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import ballerina/test;

@test:Config {
    dependsOn: [testSendSimpleEmail]
}
function testSendEmailWithOptions() returns error? {

    string host = "127.0.0.1";
    string username = "hascode";
    string password = "abcdef123";
    string subject = "Test E-Mail";
    string body = "This is a test e-mail.";
    string htmlBody = "<h1>This message is embedded in HTML tags.</h1>";
    string contentType = "text/html";
    string fromAddress = "someone1@localhost.com";
    string sender = "someone2@localhost.com";
    string[] toAddresses = ["hascode1@localhost", "hascode2@localhost"];
    string[] ccAddresses = ["hascode3@localhost", "hascode4@localhost"];
    string[] bccAddresses = ["hascode5@localhost", "hascode6@localhost"];
    string[] replyToAddresses = ["reply1@abc.com", "reply2@abc.com"];

    error? serverStatus = startSendWithOptionsSmtpServer();

    SmtpConfiguration smtpConfig = {
        port: 3025,
        security: START_TLS_NEVER
    };

    SmtpClient|Error smtpClientOrError = new (host, username,  password, smtpConfig);
    if (smtpClientOrError is Error) {
        test:assertFail(msg = "Error while initializing the SMTP client.");
    }
    SmtpClient smtpClient = check smtpClientOrError;

    //Create a text body part.
    mime:Entity bodyPart1 = new;
    bodyPart1.setText("Ballerina text body part");

    //Create a body part with json content.
    mime:Entity bodyPart2 = new;
    bodyPart2.setJson({"bodyPart":"jsonPart"});

    //Create another body part with a xml file.
    mime:Entity bodyPart3 = new;
    bodyPart3.setFileAsEntityBody("tests/resources/datafiles/file.xml", mime:TEXT_XML);

    //Create another body part with a text file.
    mime:Entity bodyPart4 = new;
    mime:ContentDisposition disposition4 = new;
    disposition4.fileName = "test.tmp";
    disposition4.disposition = "attachment";
    disposition4.name = "test";
    bodyPart4.setContentDisposition(disposition4);
    bodyPart4.setContentId("bodyPart4");
    bodyPart4.setHeader("H1", "V1");
    bodyPart4.setFileAsEntityBody("tests/resources/datafiles/test.tmp");

    //Create another body part with an image file.
    mime:Entity bodyPart5 = new;
    mime:ContentDisposition disposition5 = new;
    disposition5.fileName = "corona_virus.jpg";
    disposition5.disposition = "inline";
    bodyPart5.setContentDisposition(disposition5);
    bodyPart5.setFileAsEntityBody("tests/resources/datafiles/corona_virus.jpg", mime:IMAGE_JPEG);

    //Create another body part with binary content.
    string binaryString = "Test content";
    byte[] binary = binaryString.toBytes();
    mime:Entity bodyPart6 = new;
    bodyPart6.setByteArray(binary);

    // Create another attachment
    Attachment att7 = {filePath: "tests/resources/datafiles/vaccine.txt", contentType: "text/plain"};

    //Create an array to hold all the body parts.
    (mime:Entity|Attachment)[] bodyParts = [bodyPart1, bodyPart2, bodyPart3, bodyPart4, bodyPart5, bodyPart6, att7];

    Error? response = smtpClient->send(toAddresses, subject, fromAddress, body, cc=ccAddresses, bcc=bccAddresses,
        htmlBody=htmlBody, contentType=contentType, headers={header1_name: "header1_value"}, sender=sender,
        replyTo=replyToAddresses, attachments=bodyParts);
    if (response is Error) {
        test:assertFail(msg = "Error while sending an send-with-options email.");
    }
    Error? emailValidation = validateSendWithOptionsEmails();

    if (emailValidation is Error) {
        test:assertFail(msg = "Error while validating the received email.");
    }
    serverStatus = stopSendWithOptionsSmtpServer();
    if (serverStatus is error) {
        test:assertFail(msg = "Error while stopping send-with-options SMTP server.");
    }
    return;
}

public function startSendWithOptionsSmtpServer() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpEmailSendWithOptionsTest"
} external;

public function stopSendWithOptionsSmtpServer() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpEmailSendWithOptionsTest"
} external;

public function validateSendWithOptionsEmails() returns Error? = @java:Method {
    'class: "io.ballerina.stdlib.email.testutils.SmtpEmailSendWithOptionsTest"
} external;
