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

package org.ballerinalang.stdlib.email.testutils;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.ballerinalang.stdlib.email.util.CommonUtil;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Test class for email send using SMTP with all the parameters.
 *
 * @since slp4
 */
public class SmtpComplexEmailSendTest {

    private static final String USER_PASSWORD = "abcdef123";
    private static final String USER_NAME = "hascode";
    private static final String USER_NAME_2 = "hascode2";
    private static final String USER_NAME_3 = "hascode3";
    private static final String USER_NAME_4 = "hascode4";
    private static final String USER_NAME_5 = "hascode5";
    private static final String USER_NAME_6 = "hascode6";
    private static final String EMAIL_USER_ADDRESS_1 = "hascode1@localhost";
    private static final String EMAIL_USER_ADDRESS_2 = "hascode2@localhost";
    private static final String EMAIL_USER_ADDRESS_3 = "hascode3@localhost";
    private static final String EMAIL_USER_ADDRESS_4 = "hascode4@localhost";
    private static final String EMAIL_USER_ADDRESS_5 = "hascode5@localhost";
    private static final String EMAIL_USER_ADDRESS_6 = "hascode6@localhost";
    private static final String EMAIL_FROM = "someone1@localhost.com";
    private static final String EMAIL_SENDER = "someone2@localhost.com";
    private static final String EMAIL_SUBJECT = "Test E-Mail";
    private static final String EMAIL_TEXT = "This is a test e-mail.";
    private static final String EMAIL_CONTENT_TYPE = "text/html";
    private static final String HEADER1_NAME = "header1_name";
    private static final String HEADER1_VALUE = "header1_value";
    private static final String[] EMAIL_TO_ADDRESSES = {"hascode1@localhost", "hascode2@localhost"};
    private static final String[] EMAIL_CC_ADDRESSES = {"hascode3@localhost", "hascode4@localhost"};
    private static final String[] EMAIL_REPLY_TO_ADDRESSES = {"reply1@abc.com", "reply2@abc.com"};
    private static GreenMail mailServer;

    public static Object startComplexSmtpServer() {
        mailServer = new GreenMail(ServerSetupTest.SMTP);
        mailServer.start();
        mailServer.setUser(EMAIL_USER_ADDRESS_1, USER_NAME, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS_2, USER_NAME_2, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS_3, USER_NAME_3, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS_4, USER_NAME_4, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS_5, USER_NAME_5, USER_PASSWORD);
        mailServer.setUser(EMAIL_USER_ADDRESS_6, USER_NAME_6, USER_PASSWORD);
        return null;
    }

    public static Object stopComplexSmtpServer() {
        mailServer.stop();
        return null;
    }

    public static Object validateComplexEmails() throws IOException, MessagingException {
        MimeMessage[] messages = mailServer.getReceivedMessages();
        assertNotNull(messages);
        assertEquals(6, messages.length);
        for (MimeMessage message : messages) {
            assertEquals(EMAIL_SUBJECT, message.getSubject());
            assertTrue(message.isMimeType("multipart/*"));
            Multipart multiPart = (Multipart) message.getContent();
            int multiPartCount = multiPart.getCount();
            assertEquals(7, multiPartCount);

            testMessageBody((MimeBodyPart) multiPart.getBodyPart(0));
            testAttachment1((MimeBodyPart) multiPart.getBodyPart(1));
            testAttachment2((MimeBodyPart) multiPart.getBodyPart(2));
            testAttachment3((MimeBodyPart) multiPart.getBodyPart(3));
            testAttachment4((MimeBodyPart) multiPart.getBodyPart(4));
            testAttachment5((MimeBodyPart) multiPart.getBodyPart(5));
            testAttachment6((MimeBodyPart) multiPart.getBodyPart(6));

            assertEquals(HEADER1_VALUE, message.getHeader(HEADER1_NAME)[0]);
            assertEquals(EMAIL_FROM, message.getFrom()[0].toString());
            assertEquals(EMAIL_SENDER, message.getSender().toString());
            assertTrue(containAddresses(message.getRecipients(Message.RecipientType.TO), EMAIL_TO_ADDRESSES));
            assertTrue(containAddresses(message.getRecipients(Message.RecipientType.CC), EMAIL_CC_ADDRESSES));
            assertTrue(containAddresses(message.getReplyTo(), EMAIL_REPLY_TO_ADDRESSES));
        }
        return null;
    }

    private static void testMessageBody(MimeBodyPart bodyPart) throws IOException, MessagingException {
        assertEquals(EMAIL_TEXT, ((String) bodyPart.getContent()));
        assertTrue(bodyPart.getContentType().startsWith(EMAIL_CONTENT_TYPE));
    }

    private static void testAttachment1(MimeBodyPart bodyPart) throws IOException, MessagingException {
        InputStream input = bodyPart.getInputStream();
        assertEquals("Ballerina text body part", convertInputStreamToString(input));
    }

    private static void testAttachment2(MimeBodyPart bodyPart) throws IOException, MessagingException {
        InputStream input = bodyPart.getInputStream();
        assertEquals("{\"bodyPart\":\"jsonPart\"}", convertInputStreamToString(input));
    }

    private static void testAttachment3(MimeBodyPart bodyPart) throws IOException, MessagingException {
        InputStream input = bodyPart.getInputStream();
        assertEquals("<name>Ballerina xml file part</name>", convertInputStreamToString(input));
        assertTrue(bodyPart.getContentType().startsWith("text/xml"));
    }

    private static void testAttachment4(MimeBodyPart bodyPart) throws MessagingException, IOException {
        assertEquals("test.tmp", bodyPart.getFileName());
        assertEquals("attachment", bodyPart.getDisposition());
        assertEquals("bodyPart4", bodyPart.getContentID());
        assertEquals("application/octet-stream", bodyPart.getContentType());
        assertEquals("7bit", bodyPart.getEncoding());
        assertEquals("7bit", bodyPart.getHeader("Content-Transfer-Encoding")[0]);
        assertEquals("attachment;name=\"test\";filename=\"test.tmp\"",
                bodyPart.getHeader("content-disposition")[0]);
        assertEquals("application/octet-stream", bodyPart.getHeader("content-type")[0]);
        assertEquals("V1", bodyPart.getHeader("H1")[0]);
        assertEquals("This is a test attachment file.",
                convertInputStreamToString((InputStream) bodyPart.getContent()));
    }

    private static void testAttachment5(MimeBodyPart bodyPart) throws MessagingException, IOException {
        assertEquals("corona_virus.jpg", bodyPart.getFileName());
        assertEquals("inline", bodyPart.getDisposition());
        assertEquals("image/jpeg", bodyPart.getContentType());
        assertEquals("base64", bodyPart.getEncoding());
        compareInputStreams(new FileInputStream("src/email/tests/resources/datafiles/corona_virus.jpg"),
                (InputStream) bodyPart.getContent());
    }

    private static void testAttachment6(MimeBodyPart bodyPart) throws IOException, MessagingException {
        assertEquals("application/octet-stream", bodyPart.getContentType());
        assertEquals("Test content".getBytes(),
                CommonUtil.convertInputStreamToByteArray((SharedByteArrayInputStream) bodyPart.getContent()));
    }

    public static void compareInputStreams(InputStream input1, InputStream input2) {
        try {
            byte[] buffer1 = new byte[1024];
            byte[] buffer2 = new byte[1024];
            try {
                int numRead1;
                int numRead2;
                while (true) {
                    numRead1 = input1.read(buffer1);
                    numRead2 = input2.read(buffer2);
                    if (numRead1 > -1) {
                        if (numRead2 != numRead1 || !Arrays.equals(buffer1, buffer2)) {
                            fail();
                            break;
                        }
                    } else {
                        if (numRead2 < 0) {
                            assertTrue(true);
                        } else {
                            fail();
                        }
                        break;
                    }
                }
            } finally {
                input1.close();
            }
        } catch (IOException | RuntimeException e) {
            fail();
        } finally {
            try {
                input2.close();
            } catch (IOException e) {
                fail();
            }
        }
    }

    private static String convertInputStreamToString(InputStream inputStream)
            throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString(StandardCharsets.UTF_8.name());
    }

    private static boolean containAddresses(Address[] receivedList, String[] realList) {
        if (receivedList != null && receivedList.length == 2) {
            String[] stringReceivedList = {receivedList[0].toString(), receivedList[1].toString()};
            Arrays.sort(stringReceivedList);
            Arrays.sort(realList);
            return Arrays.equals(stringReceivedList, realList);
        } else {
            return false;
        }
    }

}
