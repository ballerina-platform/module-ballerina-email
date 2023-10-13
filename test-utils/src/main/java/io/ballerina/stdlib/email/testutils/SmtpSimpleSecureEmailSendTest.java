/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
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

import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.ballerina.stdlib.email.util.CommonUtil;
import io.ballerina.stdlib.email.util.EmailConstants;

import java.io.IOException;
import java.security.Security;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static io.ballerina.stdlib.email.testutils.Assert.assertEquals;
import static io.ballerina.stdlib.email.testutils.Assert.assertNotNull;
import static io.ballerina.stdlib.email.testutils.Assert.assertTrue;

/**
 * Test class for email send using SMTP with least number of parameters with SSL.
 *
 * @since slp4
 */
public final class SmtpSimpleSecureEmailSendTest {

    private SmtpSimpleSecureEmailSendTest() {}

    private static final int PORT_NUMBER = 3465;
    private static final String USER_PASSWORD = "abcdef123";
    private static final String USER_NAME = "someone@localhost.com";
    private static final String EMAIL_USER_ADDRESS = "hascode@localhost";
    private static final String EMAIL_FROM = "someone@localhost.com";
    private static final String EMAIL_SUBJECT = "Test E-Mail";
    private static final String EMAIL_TEXT = "This is a test e-mail.";
    private static final String SSL_SOCKET_FACTORY_PROVIDER = "ssl.SocketFactory.provider";
    private static final int SERVER_TIMEOUT = 50000;
    private static GreenMail mailServer;

    public static Object startSimpleSecureSmtpServer() {
        Security.setProperty(SSL_SOCKET_FACTORY_PROVIDER, DummySSLSocketFactory.class.getName());
        ServerSetup setup = new ServerSetup(PORT_NUMBER, null, ServerSetup.PROTOCOL_SMTPS);
        setup.setServerStartupTimeout(SERVER_TIMEOUT);
        mailServer = new GreenMail(setup);
        mailServer.start();
        mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);
        return null;
    }

    public static Object stopSimpleSecureSmtpServer() {
        mailServer.stop();
        return null;
    }

    public static Object validateSimpleSecureEmail() {
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
