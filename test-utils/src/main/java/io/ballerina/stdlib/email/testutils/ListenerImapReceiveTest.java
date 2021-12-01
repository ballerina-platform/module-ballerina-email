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

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import io.ballerina.stdlib.email.util.CommonUtil;
import io.ballerina.stdlib.email.util.EmailConstants;

import java.security.Security;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Test class for email receipt using the listener.
 *
 * @since slp4
 */
public class ListenerImapReceiveTest {

    private static GreenMailUser user;
    private static final int PORT_NUMBER = 3993;
    private static final String USER_PASSWORD = "abcdef123";
    private static final String USER_NAME = "hascode";
    private static final String EMAIL_USER_ADDRESS = "hascode@localhost";
    private static final String EMAIL_FROM = "someone@localhost.com";
    private static final String EMAIL_SUBJECT = "Test E-Mail";
    private static final String EMAIL_TEXT = "This is a test e-mail.";
    private static final String SSL_SOCKET_FACTORY_PROVIDER = "ssl.SocketFactory.provider";
    private static final int SERVER_TIMEOUT = 5000;
    private static GreenMail mailServer;

    public static Object startImapListener() {
        startServer();
        return null;
    }

    public static Object stopImapListener() {
        mailServer.stop();
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error on the thread: " + e.getMessage());
        }
        return null;
    }

    public static Object sendEmailImapListener() {
        try {
            sendEmail();
            sendEmail();
        } catch (MessagingException e) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while sending email: " + e.getMessage());
        }
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error on the thread: " + e.getMessage());
        }
        return null;
    }

    private static void startServer() {
        Security.setProperty(SSL_SOCKET_FACTORY_PROVIDER, DummySSLSocketFactory.class.getName());
        ServerSetup setup = new ServerSetup(PORT_NUMBER, null, ServerSetup.PROTOCOL_IMAPS);
        setup.setServerStartupTimeout(SERVER_TIMEOUT);
        mailServer = new GreenMail(setup);
        mailServer.start();
        user = mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);
    }

    private static void sendEmail() throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(EMAIL_USER_ADDRESS));
        message.setSubject(EMAIL_SUBJECT);
        message.setText(EMAIL_TEXT);
        user.deliver(message);
    }

}
