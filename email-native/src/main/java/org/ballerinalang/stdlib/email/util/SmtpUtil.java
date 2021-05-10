/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.email.util;

import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.mime.nativeimpl.MimeDataSourceBuilder;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.EntityHeaderHandler;
import org.ballerinalang.mime.util.MimeConstants;
import org.ballerinalang.mime.util.MimeUtil;
import org.ballerinalang.stdlib.io.channels.TempFileIOChannel;
import org.ballerinalang.stdlib.io.channels.base.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
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
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.SSLSocketFactory;

import static io.ballerina.runtime.api.creators.ValueCreator.createObjectValue;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static org.ballerinalang.mime.util.MimeConstants.MEDIA_TYPE;
import static org.ballerinalang.mime.util.MimeConstants.TEXT_PLAIN;
import static org.ballerinalang.mime.util.MimeUtil.getContentTypeWithParameters;
import static org.ballerinalang.stdlib.email.util.EmailConstants.HTML_CONTENT_TYPE;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_CERTIFICATE;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_CERT_CIPHERS;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_CERT_PROTOCOL;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_CERT_PROTOCOL_NAME;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_CERT_PROTOCOL_VERSIONS;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_START_TLS_ALWAYS;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_START_TLS_AUTO;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_START_TLS_NEVER;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_VERIFY_HOSTNAME;

/**
 * Contains the utility functions related to the SMTP protocol.
 *
 * @since 1.2.0
 */
public class SmtpUtil {

    private static final Logger log = LoggerFactory.getLogger(SmtpUtil.class);

    /**
     * Generates the Properties object using the passed BMap.
     *
     * @param smtpConfig BMap with the configuration values
     * @param host Host address of the SMTP server
     * @return Properties Set of properties required to connect to an SMTP server
     */
    public static Properties getProperties(BMap<BString, Object> smtpConfig, String host)
            throws IOException, GeneralSecurityException {
        Properties properties = new Properties();
        properties.put(EmailConstants.PROPS_SMTP_HOST, host);
        properties.put(EmailConstants.PROPS_SMTP_PORT, Long.toString(
                smtpConfig.getIntValue(EmailConstants.PROPS_PORT)));
        properties.put(EmailConstants.PROPS_SMTP_AUTH, "true");
        BString security = smtpConfig.getStringValue(EmailConstants.PROPS_SECURITY);
        if (security != null) {
            String securityType = security.getValue();
            switch (securityType) {
                case PROPS_START_TLS_AUTO:
                    properties.put(EmailConstants.PROPS_SMTP_STARTTLS, "true");
                    properties.put(EmailConstants.PROPS_SMTP_ENABLE_SSL, "false");
                    break;
                case PROPS_START_TLS_ALWAYS:
                    properties.put(EmailConstants.PROPS_SMTP_STARTTLS, "true");
                    properties.put(EmailConstants.PROPS_SMTP_STARTTLS_REQUIRED, "true");
                    properties.put(EmailConstants.PROPS_SMTP_ENABLE_SSL, "false");
                    addBasicTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
                    break;
                case PROPS_START_TLS_NEVER:
                    properties.put(EmailConstants.PROPS_SMTP_STARTTLS, "false");
                    properties.put(EmailConstants.PROPS_SMTP_ENABLE_SSL, "false");
                    break;
                default:
                    addBasicTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
            }
        } else {
            addBasicTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
        }
        addCertificate((BMap<BString, Object>) smtpConfig.getMapValue(EmailConstants.PROPS_SECURE_SOCKET),
                properties);
        printDebugLogs(properties);
        return properties;
    }

    /**
     * Generates a MIME message to be sent as an email.
     *
     * @param session Session to which the message is attached
     * @param username User who sends the email
     * @param message Ballerina-typed data object
     * @return MimeMessage Email message as a MIME message
     * @throws MessagingException If an error occurs related to messaging operations
     * @throws IOException If an error occurs related to I/O operations
     */
    public static MimeMessage generateMessage(Session session, String username, BMap<BString, Object> message)
            throws MessagingException, IOException {
        Address[] toAddressArray = extractAddressLists(message, EmailConstants.MESSAGE_TO);
        Address[] ccAddressArray = extractAddressLists(message, EmailConstants.MESSAGE_CC);
        Address[] bccAddressArray = extractAddressLists(message, EmailConstants.MESSAGE_BCC);
        Address[] replyToAddressArray = extractAddressLists(message, EmailConstants.MESSAGE_REPLY_TO);
        String subject = message.getStringValue(EmailConstants.MESSAGE_SUBJECT).getValue();
        String messageBody = getNullCheckedString(message.getStringValue(EmailConstants.MESSAGE_MESSAGE_BODY));
        String htmlMessageBody = getNullCheckedString(message.getStringValue(EmailConstants.MESSAGE_HTML_MESSAGE_BODY));
        String bodyContentType = getNullCheckedString(message.getStringValue(EmailConstants.MESSAGE_BODY_CONTENT_TYPE));
        String fromAddress = getNullCheckedString(message.getStringValue(EmailConstants.MESSAGE_FROM));
        if (fromAddress == null || fromAddress.isEmpty()) {
            fromAddress = username;
        }
        String senderAddress = getNullCheckedString(message.getStringValue(EmailConstants.MESSAGE_SENDER));
        MimeMessage emailMessage = new MimeMessage(session);
        emailMessage.setRecipients(Message.RecipientType.TO, toAddressArray);
        if (ccAddressArray.length > 0) {
            emailMessage.setRecipients(Message.RecipientType.CC, ccAddressArray);
        }
        if (bccAddressArray.length > 0) {
            emailMessage.setRecipients(Message.RecipientType.BCC, bccAddressArray);
        }
        if (replyToAddressArray.length > 0) {
            emailMessage.setReplyTo(replyToAddressArray);
        }
        emailMessage.setSubject(subject);
        emailMessage.setFrom(new InternetAddress(fromAddress));
        if (!senderAddress.isEmpty()) {
            emailMessage.setSender(new InternetAddress(senderAddress));
        }
        Object attachments = message.get(EmailConstants.MESSAGE_ATTACHMENTS);

        if (attachments == null) {
            boolean hasTextBody = !messageBody.isEmpty();
            boolean hasHtmlBody = !htmlMessageBody.isEmpty();
            if (hasTextBody && !hasHtmlBody || !hasTextBody && hasHtmlBody) {
                if (bodyContentType.compareTo("") == 0) {
                    emailMessage.setContent(messageBody, hasTextBody ? TEXT_PLAIN : HTML_CONTENT_TYPE);
                } else {
                    emailMessage.setContent(messageBody, bodyContentType);
                }
            } else if (hasTextBody) { // hasHtmlBody is also implicitly true
                emailMessage.setContent(getAlternativeContentFromTextAndHtml(messageBody, htmlMessageBody));
            }
        } else {
            addBodyAndAttachments(emailMessage, messageBody, htmlMessageBody, attachments);
        }

        addMessageHeaders(emailMessage, message);
        return emailMessage;
    }

    protected static void addCertificate(BMap<BString, Object> secureSocket, Properties properties)
            throws IOException, GeneralSecurityException {
        if (secureSocket != null) {
            String protocolName = null;
            String[] protocolVersions = null;
            String certificatePath;
            String[] supportedCiphers = null;
            BMap<BString, Object> protocol = (BMap<BString, Object>) secureSocket.getMapValue(PROPS_CERT_PROTOCOL);
            if (protocol != null) {
                protocolName = protocol.getStringValue(PROPS_CERT_PROTOCOL_NAME).getValue();
                BArray versions = protocol.getArrayValue(PROPS_CERT_PROTOCOL_VERSIONS);
                if (versions != null) {
                    protocolVersions = versions.getStringArray();
                }
            }
            BArray ciphers = secureSocket.getArrayValue(PROPS_CERT_CIPHERS);
            if (ciphers != null) {
                supportedCiphers = ciphers.getStringArray();
            }
            certificatePath = secureSocket.getStringValue(PROPS_CERTIFICATE).getValue();
            SSLSocketFactory sslSocketFactory = CommonUtil.createSSLSocketFactory(new File(certificatePath),
                    protocolName);
            addBasicTransportSecurityProperties(sslSocketFactory, properties);
            if (protocolVersions != null) {
                properties.put(EmailConstants.PROPS_SMTP_PROTOCOLS, String.join(" ", protocolVersions));
            }
            if (supportedCiphers != null) {
                properties.put(EmailConstants.PROPS_SMTP_CIPHERSUITES, String.join(" ", supportedCiphers));
            }
            if (secureSocket.containsKey(PROPS_VERIFY_HOSTNAME)) {
                Boolean verifyHostname = secureSocket.getBooleanValue(PROPS_VERIFY_HOSTNAME);
                if (verifyHostname) {
                    properties.put(EmailConstants.PROPS_SMTP_CHECK_SERVER_IDENTITY, "true");
                } else {
                    properties.put(EmailConstants.PROPS_SMTP_CHECK_SERVER_IDENTITY, "false");
                }
            }
        }
    }

    private static void addBasicTransportSecurityProperties(SSLSocketFactory sslSocketFactory, Properties properties) {
        properties.put(EmailConstants.PROPS_SMTP_SOCKET_FACTORY, sslSocketFactory);
        properties.put(EmailConstants.PROPS_SMTP_SOCKET_FACTORY_CLASS, EmailConstants.SSL_SOCKET_FACTORY_CLASS);
        properties.put(EmailConstants.PROPS_SMTP_SOCKET_FACTORY_FALLBACK, "false");
        properties.put(EmailConstants.PROPS_SMTP_CHECK_SERVER_IDENTITY, "true");
        properties.put(EmailConstants.PROPS_SMTP_ENABLE_SSL, "true");
        properties.put(EmailConstants.PROPS_SMTP_STARTTLS, "true");
    }

    private static void addMessageHeaders(MimeMessage emailMessage, BMap<BString, Object> message)
            throws MessagingException {
        BMap<BString, BString> headers =
                (BMap<BString, BString>) message.getMapValue(EmailConstants.MESSAGE_HEADERS);
        if (headers != null) {
            BString[] headerNames = headers.getKeys();
            for (BString headerName : headerNames) {
                emailMessage.addHeader(headerName.getValue(), headers.getStringValue(headerName).getValue());
            }
        }
    }

    private static void addBodyAndAttachments(MimeMessage emailMessage, String messageBody, String htmlMessageBody,
                                              Object attachments)
            throws MessagingException, IOException {
        Multipart multipart = new MimeMultipart("mixed");
        addMultipartChild(multipart, getAlternativeContentFromTextAndHtml(messageBody, htmlMessageBody));

        if (attachments instanceof BArray) {
            BArray attachmentArray = (BArray) attachments;
            for (int i = 0; i < attachmentArray.size(); i++) {
                Object attachedEntityOrRecord = attachmentArray.get(i);
                addAttachment(attachedEntityOrRecord, multipart);
            }
        } else {
            addAttachment(attachments, multipart);
        }
        emailMessage.setContent(multipart);
    }

    private static void addMultipartChild(Multipart parent, MimeMultipart child) throws MessagingException {
        final MimeBodyPart mbp = new MimeBodyPart();
        parent.addBodyPart(mbp);
        mbp.setContent(child);
    }

    private static void addAttachment(Object attachedEntityOrRecord, Multipart multipart)
            throws IOException, MessagingException {
        if (attachedEntityOrRecord instanceof BObject) {
            BObject mimeEntity = (BObject) attachedEntityOrRecord;
            String contentType = getContentTypeWithParameters(mimeEntity);
            if (contentType.startsWith(MimeConstants.MULTIPART_AS_PRIMARY_TYPE)) {
                multipart.addBodyPart(populateMultipart(mimeEntity));
            } else {
                multipart.addBodyPart(buildJavaMailBodyPart(mimeEntity, contentType));
            }
        } else if (attachedEntityOrRecord instanceof BMap) {
            BMap<BString, BString> attachedEntity = (BMap<BString, BString>) attachedEntityOrRecord;
            String attachmentFilePath
                    = attachedEntity.getStringValue(EmailConstants.ATTACHMENT_FILE_PATH).getValue();
            String attachmentContentType
                    = attachedEntity.getStringValue(EmailConstants.ATTACHMENT_CONTENT_TYPE).getValue();
            File file = new File(attachmentFilePath);
            BObject mimeEntity = createObjectValue(MimeUtil.getMimePackage(), ENTITY);
            mimeEntity.addNativeData(ENTITY_BYTE_CHANNEL, getByteChannelForTempFile(file.getAbsolutePath()));
            MimeUtil.setContentType(createObjectValue(MimeUtil.getMimePackage(), MEDIA_TYPE), mimeEntity,
                    TEXT_PLAIN);
            if (attachmentContentType.startsWith(MimeConstants.MULTIPART_AS_PRIMARY_TYPE)) {
                multipart.addBodyPart(populateMultipart(mimeEntity));
            } else {
                multipart.addBodyPart(buildJavaMailBodyPart(mimeEntity, attachmentContentType));
            }
        }
    }

    private static TempFileIOChannel getByteChannelForTempFile(String temporaryFilePath) throws IOException {
        FileChannel fileChannel;
        Set<OpenOption> options = new HashSet<>();
        options.add(StandardOpenOption.READ);
        Path path = Paths.get(temporaryFilePath);
        fileChannel = (FileChannel) Files.newByteChannel(path, options);
        return new TempFileIOChannel(fileChannel, temporaryFilePath);
    }

    private static MimeBodyPart populateMultipart(BObject mimeEntity) throws IOException, MessagingException {
        Multipart multipart = new MimeMultipart();
        BArray multipartMimeEntityArrayValue = EntityBodyHandler.getBodyPartArray(mimeEntity);
        int entityCount = multipartMimeEntityArrayValue.size();
        for (int i = 0; i < entityCount; i++) {
            BObject childMimeEntity = (BObject) multipartMimeEntityArrayValue.get(i);
            String childContentType = getContentTypeWithParameters(childMimeEntity);
            if (childContentType.startsWith(MimeConstants.MULTIPART_AS_PRIMARY_TYPE)) {
                multipart.addBodyPart(populateMultipart(childMimeEntity));
            } else {
                multipart.addBodyPart(buildJavaMailBodyPart(childMimeEntity, childContentType));
            }
        }
        MimeBodyPart returnMimeBodyPart = new MimeBodyPart();
        returnMimeBodyPart.setContent(multipart);
        return returnMimeBodyPart;
    }

    private static MimeBodyPart buildJavaMailBodyPart(BObject mimeEntity, String contentType)
            throws MessagingException, IOException {
        MimeBodyPart attachmentBodyPart = new MimeBodyPart();
        Channel channel = EntityBodyHandler.getByteChannel(mimeEntity);
        if (channel != null) {
            InputStream inputStream = channel.getInputStream();
            ByteArrayDataSource ds = new ByteArrayDataSource(inputStream, contentType);
            attachmentBodyPart.setDataHandler(new DataHandler(ds));
        } else {
            if (CommonUtil.isTextBased(contentType)) {
                attachmentBodyPart.setText(((BString) MimeDataSourceBuilder.getText(mimeEntity)).getValue());
            } else {
                BArray binaryContent = (BArray) MimeDataSourceBuilder.getByteArray(mimeEntity);
                attachmentBodyPart.setContent(binaryContent.getBytes(), MimeConstants.OCTET_STREAM);
            }
        }
        addHeadersToJavaMailBodyPart(mimeEntity, attachmentBodyPart);
        return attachmentBodyPart;
    }

    private static void addHeadersToJavaMailBodyPart(BObject mimeEntity, MimeBodyPart attachmentBodyPart)
            throws MessagingException {

        BMap<BString, Object> entityHeaders = EntityHeaderHandler.getEntityHeaderMap(mimeEntity);

        for (BString entryKey : entityHeaders.getKeys()) {
            BArray entryValues = (BArray) entityHeaders.get(entryKey);
            if (entryValues.size() > 0) {
                String headerName = entryKey.getValue();
                String headerValue = entryValues.getBString(0).getValue();
                if (isNotEmpty(headerName)) {
                    log.debug("Added a MIME body part header " + headerName + " with value " + headerValue);
                    attachmentBodyPart.setHeader(headerName, headerValue);
                }
            }
        }
    }

    private static Address[] extractAddressLists(BMap<BString, Object> message, BString addressType)
            throws AddressException {
        String[] address =  getNullCheckedStringArray(message, addressType);
        int addressArrayLength = address.length;
        Address[] addressArray = new Address[addressArrayLength];
        for (int i = 0; i < addressArrayLength; i++) {
            addressArray[i] = new InternetAddress(address[i]);
        }
        return addressArray;
    }

    private static String[] getNullCheckedStringArray(BMap<BString, Object> mapValue, BString parameter) {
        if (mapValue != null) {
            Object parameterValue = mapValue.get(parameter);
            if (parameterValue != null) {
                if (parameterValue instanceof BArray) {
                    return ((BArray) parameterValue).getStringArray();
                } else if (parameterValue instanceof BString) {
                    return new String[]{((BString) parameterValue).getValue()};
                } else {
                    return new String[0];
                }
            } else {
                return new String[0];
            }
        } else {
            return new String[0];
        }
    }

    private static MimeMultipart getAlternativeContentFromTextAndHtml(String textContent, String htmlContent)
            throws MessagingException {
        MimeMultipart multipart = new MimeMultipart("alternative");
        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(textContent);
        multipart.addBodyPart(messageBodyPart);
        messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(htmlContent, HTML_CONTENT_TYPE);
        multipart.addBodyPart(messageBodyPart);
        return multipart;
    }

    private static String getNullCheckedString(BString string) {
        return string == null ? "" : string.getValue();
    }

    private static boolean isNotEmpty(String string) {
        return string != null && !string.isEmpty();
    }

    @ExcludeCoverageFromGeneratedReport
    private static void printDebugLogs(Properties properties) {
        if (log.isDebugEnabled()) {
            Set<String> propertySet = properties.stringPropertyNames();
            log.debug("SMTP Properties set are as follows.");
            for (Object propertyObj : propertySet) {
                log.debug("Property Name: " + propertyObj + ", Value: " + properties.get(propertyObj).toString()
                        + " ValueType: " + properties.get(propertyObj).getClass().getName());
            }
        }
    }

}
