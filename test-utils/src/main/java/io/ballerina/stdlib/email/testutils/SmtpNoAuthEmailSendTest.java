/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.email.testutils;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.ballerina.stdlib.email.util.CommonUtil;
import io.ballerina.stdlib.email.util.EmailConstants;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static io.ballerina.stdlib.email.testutils.Assert.assertEquals;
import static io.ballerina.stdlib.email.testutils.Assert.assertNotNull;

/**
 * Test class for email send using SMTP without authentication.
 *
 * @since 2.13.0
 */
public final class SmtpNoAuthEmailSendTest {

    private SmtpNoAuthEmailSendTest() {}

    private static final String EMAIL_USER_ADDRESS = "hascode@localhost";
    private static final String EMAIL_FROM = "someone@localhost.com";
    private static final String EMAIL_SUBJECT = "Test E-Mail Without Auth";
    private static final String EMAIL_TEXT = "This is a test e-mail sent without authentication.";
    private static final int SMTP_PORT = 3026;
    private static GreenMail mailServer;

    public static Object startNoAuthSmtpServer() {
        ServerSetup serverSetup = new ServerSetup(SMTP_PORT, null, ServerSetup.PROTOCOL_SMTP);
        serverSetup.setServerStartupTimeout(10000);
        mailServer = new GreenMail(serverSetup);
        mailServer.start();
        return null;
    }

    public static Object stopNoAuthSmtpServer() {
        if (mailServer != null) {
            mailServer.stop();
        }
        return null;
    }

    public static Object validateNoAuthEmail() {
        MimeMessage[] messages = mailServer.getReceivedMessages();
        assertNotNull(messages);
        if (messages.length < 1) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "No messages received by the SMTP server without authentication");
        }
        try {
            MimeMessage message = messages[0];
            assertEquals(EMAIL_SUBJECT, message.getSubject());
            assertNotNull(message.getFrom());
            assertEquals(EMAIL_FROM, message.getFrom()[0].toString());
            assertNotNull(message.getRecipients(Message.RecipientType.TO));
            assertEquals(EMAIL_USER_ADDRESS, message.getRecipients(Message.RecipientType.TO)[0].toString());
            assertEquals(EMAIL_TEXT, message.getContent().toString().trim());
        } catch (MessagingException | java.io.IOException e) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while validating the email sent without authentication: " + e.getMessage());
        }
        return null;
    }
}
