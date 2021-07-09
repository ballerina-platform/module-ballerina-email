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
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import io.ballerina.stdlib.mime.util.MimeConstants;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Test class for email receive using IMAP with all the parameters.
 *
 * @since slp4
 */
public class ImapComplexEmailReceiveTest {

    private static GreenMailUser user;
    private static final String USER_PASSWORD = "abcdef123";
    private static final String USER_NAME = "hascode";
    private static final String EMAIL_USER_ADDRESS = "hascode@localhost";
    private static final String EMAIL_FROM = "someone@localhost.com";
    private static final String EMAIL_SENDER = "someone2@localhost.com";
    private static final String EMAIL_SUBJECT = "Test E-Mail";
    private static final String EMAIL_TEXT = "This is a test e-mail.";
    private static final String HEADER1_NAME = "header1_name";
    private static final String HEADER1_VALUE = "header1_value";
    private static final String ATTACHMENT1_TEXT = "Sample attachment text";
    private static final String ATTACHMENT2_TEXT = "{\"bodyPart\":\"jsonPart\"}";
    private static final String ATTACHMENT3_TEXT = "<name>Ballerina xml file part</name>";
    private static final byte[] ATTACHMENT4_BINARY = "This is a sample source of bytes.".getBytes();
    private static final String ATTACHMENT1_HEADER1_NAME_TEXT = "H1";
    private static final String ATTACHMENT1_HEADER1_VALUE_TEXT = "V1";
    private static final String MULTIPART_JSON = "{\"multipartJson\":\"sampleValue\"}";
    private static final String MULTIPART_XML = "<name>Ballerina Multipart XML</name>";
    private static final String[] EMAIL_TO_ADDRESSES = {"hascode1@localhost", "hascode2@localhost"};
    private static final String[] EMAIL_CC_ADDRESSES = {"hascode3@localhost", "hascode4@localhost"};
    private static final String[] EMAIL_BCC_ADDRESSES = {"hascode5@localhost", "hascode6@localhost"};
    private static final String[] EMAIL_REPLY_TO_ADDRESSES = {"reply1@abc.com", "reply2@abc.com"};
    private static GreenMail mailServer;

    public static Object startComplexImapServer() {
        startServer();
        return null;
    }

    public static Object stopComplexImapServer() {
        mailServer.stop();
        return null;
    }

    public static Object sendEmailComplexImapServer() throws MessagingException {
        sendEmail();
        return null;
    }

    private static void startServer() {
        mailServer = new GreenMail(ServerSetupTest.IMAP);
        mailServer.start();
        user = mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS, USER_NAME, USER_PASSWORD);
    }

    private static void sendEmail() throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.setSender(new InternetAddress(EMAIL_SENDER));
        message.addRecipients(Message.RecipientType.TO, convertToAddressArray(EMAIL_TO_ADDRESSES));
        message.addRecipients(Message.RecipientType.CC, convertToAddressArray(EMAIL_CC_ADDRESSES));
        message.addRecipients(Message.RecipientType.BCC, convertToAddressArray(EMAIL_BCC_ADDRESSES));
        message.setReplyTo(convertToAddressArray(EMAIL_REPLY_TO_ADDRESSES));
        message.setSubject(EMAIL_SUBJECT);

        MimeMultipart multipartMessage = new MimeMultipart();
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(EMAIL_TEXT, MimeConstants.TEXT_PLAIN);
        MimeBodyPart attachment1 = new MimeBodyPart();
        MimeBodyPart attachment2 = new MimeBodyPart();
        MimeBodyPart attachment3 = new MimeBodyPart();
        MimeBodyPart attachment4 = new MimeBodyPart();
        attachment1.setContent(ATTACHMENT1_TEXT, MimeConstants.TEXT_PLAIN);
        attachment1.addHeader(ATTACHMENT1_HEADER1_NAME_TEXT, ATTACHMENT1_HEADER1_VALUE_TEXT);
        attachment2.setContent(ATTACHMENT2_TEXT, MimeConstants.APPLICATION_JSON);
        attachment3.setContent(ATTACHMENT3_TEXT, MimeConstants.APPLICATION_XML);
        attachment4.setContent(ATTACHMENT4_BINARY, MimeConstants.OCTET_STREAM);
        multipartMessage.addBodyPart(messageBodyPart);
        multipartMessage.addBodyPart(attachment1);
        multipartMessage.addBodyPart(attachment2);
        multipartMessage.addBodyPart(attachment3);
        multipartMessage.addBodyPart(attachment4);

        Multipart multipartAttachment = new MimeMultipart();
        MimeBodyPart multipartJson = new MimeBodyPart();
        MimeBodyPart multipartXml = new MimeBodyPart();
        multipartJson.setContent(MULTIPART_JSON, MimeConstants.APPLICATION_JSON);
        multipartXml.setContent(MULTIPART_XML, MimeConstants.APPLICATION_XML);
        multipartAttachment.addBodyPart(multipartJson);
        multipartAttachment.addBodyPart(multipartXml);
        MimeBodyPart mimeAttachment = new MimeBodyPart();
        mimeAttachment.setContent(multipartAttachment);
        multipartMessage.addBodyPart(mimeAttachment);
        message.setContent(multipartMessage);

        message.addHeader(HEADER1_NAME, HEADER1_VALUE);
        user.deliver(message);

    }

    private static Address[] convertToAddressArray(String[] stringAddresses) throws AddressException {
        if (stringAddresses != null && stringAddresses.length > 0) {
            Address[] addresses = new Address[stringAddresses.length];
            for (int i = 0; i < stringAddresses.length; i++) {
                addresses[i] = new InternetAddress(stringAddresses[i]);
            }
            return addresses;
        } else {
            return null;
        }
    }

}
