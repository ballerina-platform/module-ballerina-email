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

import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static io.ballerina.stdlib.email.testutils.Assert.assertEquals;
import static io.ballerina.stdlib.email.testutils.Assert.assertNotNull;
import static io.ballerina.stdlib.email.testutils.Assert.assertTrue;

/**
 * Test class for email send using SMTP with no authentication.
 *
 * @since 2.13.0
 */
public final class SmtpNoAuthEmailSendTest {

    private SmtpNoAuthEmailSendTest() {}

    private static final int PORT_NUMBER = 3025;
    private static final String EMAIL_FROM = "sender@localhost.com";
    private static final String EMAIL_SUBJECT = "Test E-Mail No Auth";
    private static final String EMAIL_TEXT = "This is a test e-mail sent with no-auth server.";
    private static final int SERVER_TIMEOUT = 50000;
    private static GreenMail mailServer;

    public static Object startNoAuthSmtpServer() {
        ServerSetup setup = new ServerSetup(PORT_NUMBER, null, ServerSetup.PROTOCOL_SMTP);
        setup.setServerStartupTimeout(SERVER_TIMEOUT);
        mailServer = new GreenMail(setup);
        mailServer.start();
        // No user setup required for no-auth SMTP
        return null;
    }

    public static Object stopNoAuthSmtpServer() {
        mailServer.stop();
        return null;
    }

    public static Object validateNoAuthEmail() {
        MimeMessage[] messages = mailServer.getReceivedMessages();
        assertNotNull(messages);
        assertEquals(1, messages.length);
        MimeMessage message = messages[0];
        try {
            assertEquals(EMAIL_SUBJECT, message.getSubject());
            assertTrue(String.valueOf(message.getContent()).contains(EMAIL_TEXT));
            assertEquals(EMAIL_FROM, message.getFrom()[0].toString());
        } catch (MessagingException | IOException e) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while validating the simple secure email: " + e.getMessage());
        }
        return null;
    }
}
