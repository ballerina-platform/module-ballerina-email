/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import java.io.IOException;
import java.security.Security;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Test class for email send using SMTP with least number of parameters with STARTTLS.
 *
 * @since slbeta1
 */
public class SmtpSimpleSecureEmailSendWithOptionsTest {

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

    public static Object startSimpleSecureSmtpServerWithOptions() {
        Security.setProperty(SSL_SOCKET_FACTORY_PROVIDER, DummySSLSocketFactory.class.getName());
        ServerSetup setup = new ServerSetup(PORT_NUMBER, null, ServerSetup.PROTOCOL_SMTPS);
        setup.setServerStartupTimeout(SERVER_TIMEOUT);
        mailServer = new GreenMail(setup);
        mailServer.start();
        mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);
        return null;
    }

    public static Object stopSimpleSecureSmtpServerWithOptions() {
        mailServer.stop();
        return null;
    }

    public static Object validateSimpleSecureEmailWithOptions() throws IOException, MessagingException, InterruptedException {
        MimeMessage[] messages = mailServer.getReceivedMessages();
        assertNotNull(messages);
        assertEquals(1, messages.length);
        MimeMessage message = messages[0];
        assertEquals(EMAIL_SUBJECT, message.getSubject());
        assertTrue(String.valueOf(message.getContent()).contains(EMAIL_TEXT));
        assertEquals(EMAIL_FROM, message.getFrom()[0].toString());
        return null;
    }

}
