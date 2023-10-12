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

package io.ballerina.stdlib.email.util;

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.pop3.POP3Message;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXmlSequence;
import io.ballerina.stdlib.mime.util.EntityBodyChannel;
import io.ballerina.stdlib.mime.util.EntityWrapper;
import io.ballerina.stdlib.mime.util.HeaderUtil;
import io.ballerina.stdlib.mime.util.MimeConstants;
import io.ballerina.stdlib.mime.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.net.ssl.SSLSocketFactory;

import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_CERTIFICATE;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_CERT_CIPHERS;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_CERT_PROTOCOL;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_CERT_PROTOCOL_NAME;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_CERT_PROTOCOL_VERSIONS;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_START_TLS_ALWAYS;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_START_TLS_AUTO;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_START_TLS_NEVER;
import static io.ballerina.stdlib.email.util.EmailConstants.PROPS_VERIFY_HOSTNAME;
import static io.ballerina.stdlib.mime.util.MimeConstants.BODY_PARTS;
import static io.ballerina.stdlib.mime.util.MimeConstants.ENTITY;
import static io.ballerina.stdlib.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static io.ballerina.stdlib.mime.util.MimeConstants.MEDIA_TYPE;

/**
 * Contains utility functions related to the POP and IMAP protocols.
 *
 * @since 1.2.0
 */
public final class EmailAccessUtil {

    private EmailAccessUtil() {}

    private static final Logger log = LoggerFactory.getLogger(EmailAccessUtil.class);
    private static final ArrayType stringArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING);

    /**
     * Generates Properties object using the passed BMap.
     *
     * @param emailAccessConfig BMap with the configuration values
     * @param host Host address of email server
     * @return Properties Email server access properties
     */
    public static Properties getPopProperties(BMap<BString, Object> emailAccessConfig, String host)
            throws GeneralSecurityException, IOException {
        Properties properties = new Properties();
        properties.put(EmailConstants.PROPS_POP_HOST, host);
        properties.put(EmailConstants.PROPS_POP_PORT,
                Long.toString(emailAccessConfig.getIntValue(EmailConstants.PROPS_PORT)));
        BString security = emailAccessConfig.getStringValue(EmailConstants.PROPS_SECURITY);
        String securityType = security.getValue();
        switch (securityType) {
            case PROPS_START_TLS_AUTO:
                properties.put(EmailConstants.PROPS_POP_STARTTLS, "true");
                properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "false");
                break;
            case PROPS_START_TLS_ALWAYS:
                properties.put(EmailConstants.PROPS_POP_STARTTLS, "true");
                properties.put(EmailConstants.PROPS_POP_STARTTLS_REQUIRED, "true");
                properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "false");
                addBasicPopTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
                break;
            case PROPS_START_TLS_NEVER:
                properties.put(EmailConstants.PROPS_POP_STARTTLS, "false");
                properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "false");
                break;
            default:
                addBasicPopTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
        }
        addPopCertificate((BMap<BString, Object>) emailAccessConfig.getMapValue
                (EmailConstants.PROPS_SECURE_SOCKET), properties);
        properties.put(EmailConstants.PROPS_POP_AUTH, "true");
        properties.put(EmailConstants.MAIL_STORE_PROTOCOL, EmailConstants.POP_PROTOCOL);
        printPopDebugLogs(properties);
        return properties;
    }

    /**
     * Generates Properties object using the passed BMap.
     *
     * @param emailAccessConfig BMap with the configuration values
     * @param host Host address of email server
     * @return Properties Email server access properties
     */
    public static Properties getImapProperties(BMap<BString, Object> emailAccessConfig, String host)
            throws GeneralSecurityException, IOException {
        Properties properties = new Properties();
        properties.put(EmailConstants.PROPS_IMAP_HOST, host);
        properties.put(EmailConstants.PROPS_IMAP_PORT,
                Long.toString(emailAccessConfig.getIntValue(EmailConstants.PROPS_PORT)));
        BString security = emailAccessConfig.getStringValue(EmailConstants.PROPS_SECURITY);
        String securityType = security.getValue();
        switch (securityType) {
            case PROPS_START_TLS_AUTO:
                properties.put(EmailConstants.PROPS_IMAP_STARTTLS, "true");
                properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "false");
                break;
            case PROPS_START_TLS_ALWAYS:
                properties.put(EmailConstants.PROPS_IMAP_STARTTLS, "true");
                properties.put(EmailConstants.PROPS_IMAP_STARTTLS_REQUIRED, "true");
                properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "false");
                addBasicImapTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
                break;
            case PROPS_START_TLS_NEVER:
                properties.put(EmailConstants.PROPS_IMAP_STARTTLS, "false");
                properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "false");
                break;
            default:
                addBasicImapTransportSecurityProperties(CommonUtil.createDefaultSSLSocketFactory(), properties);
        }
        properties.put(EmailConstants.PROPS_IMAP_AUTH, "true");
        properties.put(EmailConstants.MAIL_STORE_PROTOCOL, EmailConstants.IMAP_PROTOCOL);
        addImapCertificate((BMap<BString, Object>) emailAccessConfig.getMapValue
                (EmailConstants.PROPS_SECURE_SOCKET), properties);
        printImapDebugLogs(properties);
        return properties;
    }

    /**
     * Generates BMap object using the passed message.
     *
     * @param message Email message received
     * @return BMap Ballerina compatible map object
     * @throws MessagingException If an error occurs related to messaging
     * @throws IOException If an error occurs related to I/O
     */
    public static BMap<BString, Object> getMapValue(Message message) throws MessagingException, IOException {
        Map<String, Object> valueMap = new HashMap<>();
        Object toAddressArrayValue = getAddressBArrayList(message.getRecipients(Message.RecipientType.TO));
        Object ccAddressArrayValue = getAddressBArrayList(message.getRecipients(Message.RecipientType.CC));
        Object bccAddressArrayValue = getAddressBArrayList(message.getRecipients(Message.RecipientType.BCC));
        Object replyToAddressArrayValue = getAddressBArrayList(message.getReplyTo());
        String subject = getStringNullChecked(message.getSubject());
        String messageBody = extractBodyFromMessage(message);
        BMap<BString, Object> headers = extractHeadersFromMessage(message);
        String messageContentType = message.getContentType();
        String fromAddress = extractFromAddressFromMessage(message);
        String senderAddress = getSenderAddress(message);
        BArray attachments = extractAttachmentsFromMessage(message);
        valueMap.put(EmailConstants.MESSAGE_TO.getValue(), toAddressArrayValue);
        valueMap.put(EmailConstants.MESSAGE_CC.getValue(), ccAddressArrayValue);
        valueMap.put(EmailConstants.MESSAGE_BCC.getValue(), bccAddressArrayValue);
        valueMap.put(EmailConstants.MESSAGE_REPLY_TO.getValue(), replyToAddressArrayValue);
        valueMap.put(EmailConstants.MESSAGE_SUBJECT.getValue(), subject);
        if (CommonUtil.isJsonBased(message.getContentType())) {
            valueMap.put(EmailConstants.MESSAGE_MESSAGE_BODY.getValue(), getJsonContent(messageBody));
        } else if (CommonUtil.isXmlBased(message.getContentType())) {
            valueMap.put(EmailConstants.MESSAGE_MESSAGE_BODY.getValue(), parseToXml(messageBody));
        } else {
            valueMap.put(EmailConstants.MESSAGE_MESSAGE_BODY.getValue(), messageBody);
        }
        if (messageContentType != null && !messageContentType.equals("")) {
            valueMap.put(EmailConstants.MESSAGE_BODY_CONTENT_TYPE.getValue(), messageContentType);
        }
        if (headers != null) {
            valueMap.put(EmailConstants.MESSAGE_HEADERS.getValue(), headers);
        }
        valueMap.put(EmailConstants.MESSAGE_FROM.getValue(), fromAddress);
        valueMap.put(EmailConstants.MESSAGE_SENDER.getValue(), senderAddress);
        if (attachments != null && attachments.size() > 0) {
            valueMap.put(EmailConstants.MESSAGE_ATTACHMENTS.getValue(), attachments);
        }
        return ValueCreator.createRecordValue(EmailUtils.getEmailPackage(), EmailConstants.EMAIL_MESSAGE, valueMap);
    }

    protected static void addPopCertificate(BMap<BString, Object> secureSocket, Properties properties)
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
            properties.put(EmailConstants.PROPS_POP_SOCKET_FACTORY, sslSocketFactory);
            properties.put(EmailConstants.PROPS_POP_SOCKET_FACTORY_CLASS, EmailConstants.SSL_SOCKET_FACTORY_CLASS);
            properties.put(EmailConstants.PROPS_POP_SOCKET_FACTORY_FALLBACK, "false");
            properties.put(EmailConstants.PROPS_POP_CHECK_SERVER_IDENTITY, "true");
            properties.put(EmailConstants.PROPS_SMTP_ENABLE_SSL, "true");
            properties.put(EmailConstants.PROPS_POP_STARTTLS, "true");
            if (protocolVersions != null) {
                properties.put(EmailConstants.PROPS_POP_PROTOCOLS, String.join(" ", protocolVersions));
            }
            if (supportedCiphers != null) {
                properties.put(EmailConstants.PROPS_POP_CIPHERSUITES, String.join(" ", supportedCiphers));
            }
            if (secureSocket.containsKey(PROPS_VERIFY_HOSTNAME)) {
                Boolean verifyHostname = secureSocket.getBooleanValue(PROPS_VERIFY_HOSTNAME);
                if (verifyHostname) {
                    properties.put(EmailConstants.PROPS_POP_CHECK_SERVER_IDENTITY, "true");
                } else {
                    properties.put(EmailConstants.PROPS_POP_CHECK_SERVER_IDENTITY, "false");
                }
            }
        }
    }

    protected static void addImapCertificate(BMap<BString, Object> secureSocket, Properties properties)
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
            properties.put(EmailConstants.PROPS_IMAP_SOCKET_FACTORY, sslSocketFactory);
            properties.put(EmailConstants.PROPS_IMAP_SOCKET_FACTORY_CLASS, EmailConstants.SSL_SOCKET_FACTORY_CLASS);
            properties.put(EmailConstants.PROPS_IMAP_SOCKET_FACTORY_FALLBACK, "false");
            properties.put(EmailConstants.PROPS_IMAP_CHECK_SERVER_IDENTITY, "true");
            properties.put(EmailConstants.PROPS_SMTP_ENABLE_SSL, "true");
            properties.put(EmailConstants.PROPS_IMAP_STARTTLS, "true");
            if (protocolVersions != null) {
                properties.put(EmailConstants.PROPS_IMAP_PROTOCOLS, String.join(" ", protocolVersions));
            }
            if (supportedCiphers != null) {
                properties.put(EmailConstants.PROPS_IMAP_CIPHERSUITES, String.join(" ", supportedCiphers));
            }
            if (secureSocket.containsKey(PROPS_VERIFY_HOSTNAME)) {
                Boolean verifyHostname = secureSocket.getBooleanValue(PROPS_VERIFY_HOSTNAME);
                if (verifyHostname) {
                    properties.put(EmailConstants.PROPS_IMAP_CHECK_SERVER_IDENTITY, "true");
                } else {
                    properties.put(EmailConstants.PROPS_IMAP_CHECK_SERVER_IDENTITY, "false");
                }
            }
        }
    }

    private static void addBasicPopTransportSecurityProperties(SSLSocketFactory sslSocketFactory,
                                                               Properties properties) {
        properties.put(EmailConstants.PROPS_POP_SOCKET_FACTORY, sslSocketFactory);
        properties.put(EmailConstants.PROPS_POP_SOCKET_FACTORY_CLASS, EmailConstants.SSL_SOCKET_FACTORY_CLASS);
        properties.put(EmailConstants.PROPS_POP_SOCKET_FACTORY_FALLBACK, "false");
        properties.put(EmailConstants.PROPS_POP_CHECK_SERVER_IDENTITY, "true");
        properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "true");
        properties.put(EmailConstants.PROPS_POP_STARTTLS, "true");
    }

    private static void addBasicImapTransportSecurityProperties(SSLSocketFactory sslSocketFactory,
                                                                Properties properties) {
        properties.put(EmailConstants.PROPS_IMAP_SOCKET_FACTORY, sslSocketFactory);
        properties.put(EmailConstants.PROPS_IMAP_SOCKET_FACTORY_CLASS, EmailConstants.SSL_SOCKET_FACTORY_CLASS);
        properties.put(EmailConstants.PROPS_IMAP_SOCKET_FACTORY_FALLBACK, "false");
        properties.put(EmailConstants.PROPS_IMAP_CHECK_SERVER_IDENTITY, "true");
        properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "true");
        properties.put(EmailConstants.PROPS_IMAP_STARTTLS, "true");
    }

    private static BMap<BString, Object> extractHeadersFromMessage(Message message) throws MessagingException {
        BMap<BString, Object> headerMap
                = ValueCreator.createMapValue(TypeCreator.createMapType(PredefinedTypes.TYPE_STRING));
        Enumeration<Header> headers = message.getAllHeaders();
        if (headers.hasMoreElements()) {
            while (headers.hasMoreElements()) {
                Header header = headers.nextElement();
                headerMap.put(StringUtils.fromString(header.getName()), StringUtils.fromString(header.getValue()));
            }
            return headerMap;
        }
        return null;
    }

    private static BXmlSequence parseToXml(String xmlStr) {
        return (BXmlSequence) XmlUtils.parse(xmlStr);
    }

    private static Object getJsonContent(String messageContent) {
        Object json = JsonUtils.parse(messageContent);
        if (json instanceof String) {
            return StringUtils.fromString((String) json);
        }
        return json;
    }

    private static String extractBodyFromMessage(Message message) throws MessagingException, IOException {
        String contentType = message.getContentType();
        if (contentType != null && CommonUtil.isTextBased(contentType.toLowerCase(Locale.getDefault()))) {
            Object content = message.getContent();
            if (content == null) {
                return "";
            }
            if (content instanceof InputStream) {
                InputStream contentStream = (InputStream) content;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = contentStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                contentStream.close();
                return buffer.toString(StandardCharsets.UTF_8);
            } else {
                return content.toString();
            }
        } else if (message.isMimeType(EmailConstants.MIME_CONTENT_TYPE_PATTERN)) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            if (mimeMultipart != null && mimeMultipart.getCount() > 0 && mimeMultipart.getBodyPart(0) != null
                    && mimeMultipart.getBodyPart(0).getContent() != null) {
                Object messageObject = mimeMultipart.getBodyPart(0).getContent();
                if (messageObject instanceof String) {
                    return (String) messageObject;
                }
            }
        }
        return "";
    }

    private static BArray extractAttachmentsFromMessage(Message message) throws MessagingException, IOException {
        ArrayList<BObject> attachmentArray = new ArrayList<>();
        if (!message.isMimeType(EmailConstants.MIME_CONTENT_TYPE_PATTERN)) {
            return null;
        } else {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            int numberOfAttachments = mimeMultipart.getCount();
            if (numberOfAttachments > 1) {
                for (int i = 1; i < numberOfAttachments; i++) {
                    attachMultipart(mimeMultipart.getBodyPart(i), attachmentArray);
                }
                return getArrayOfEntities(attachmentArray);
            } else {
                log.debug("Received a Multipart email message without any attachments.");
                return null;
            }
        }
    }

    private static void attachMultipart(BodyPart bodyPart, ArrayList<BObject> entityArray)
            throws MessagingException, IOException {
        if (bodyPart.isMimeType(EmailConstants.MIME_CONTENT_TYPE_PATTERN)) {
            entityArray.add(getMultipartEntity(bodyPart));
        } else {
            String contentType = bodyPart.getContentType();
            if (contentType != null) {
                if (CommonUtil.isJsonBased(contentType)) {
                    entityArray.add(getTypedEntity(bodyPart, MimeConstants.APPLICATION_JSON));
                } else if (CommonUtil.isXmlBased(contentType)) {
                    entityArray.add(getTypedEntity(bodyPart, MimeConstants.APPLICATION_XML));
                } else if (CommonUtil.isTextBased(contentType)) {
                    entityArray.add(getTypedEntity(bodyPart, MimeConstants.TEXT_PLAIN));
                } else {
                    entityArray.add(getTypedEntity(bodyPart, MimeConstants.OCTET_STREAM));
                }
            } else {
                entityArray.add(getTypedEntity(bodyPart, MimeConstants.OCTET_STREAM));
            }
        }
    }

    private static BObject getMultipartEntity(BodyPart bodyPart) throws MessagingException, IOException {
        BObject multipartEntity = createEntityObject();
        ArrayList<BObject> entities = getMultipleEntities(bodyPart);
        if (entities != null && bodyPart.getContentType() != null) {
            multipartEntity.addNativeData(BODY_PARTS, getArrayOfEntities(entities));
            MimeUtil.setContentType(createMediaTypeObject(), multipartEntity, bodyPart.getContentType());
            setEntityHeaders(multipartEntity, bodyPart);
        }
        return multipartEntity;
    }

    private static ArrayList<BObject> getMultipleEntities(BodyPart bodyPart)
            throws IOException, MessagingException {
        ArrayList<BObject> entityArray = new ArrayList<>();
        MimeMultipart mimeMultipart = (MimeMultipart) bodyPart.getContent();
        int numberOfBodyParts = mimeMultipart.getCount();
        if (numberOfBodyParts > 0) {
            for (int i = 0; i < numberOfBodyParts; i++) {
                BodyPart subPart = mimeMultipart.getBodyPart(i);
                attachMultipart(subPart, entityArray);
            }
            return entityArray;
        } else {
            return null;
        }
    }

    private static BObject getTypedEntity(BodyPart bodyPart, String mimeType) throws IOException, MessagingException {
        byte[] binaryContent = CommonUtil.convertInputStreamToByteArray(bodyPart.getInputStream());
        EntityWrapper byteChannel = new EntityWrapper(new EntityBodyChannel(new ByteArrayInputStream(binaryContent)));
        BObject entity = createEntityObject();
        entity.addNativeData(ENTITY_BYTE_CHANNEL, byteChannel);
        MimeUtil.setContentType(createMediaTypeObject(), entity, mimeType);
        setEntityHeaders(entity, bodyPart);
        return entity;
    }

    private static void setEntityHeaders(BObject entity, BodyPart bodyPart) throws MessagingException {
        Enumeration<Header> headers = bodyPart.getAllHeaders();
        while (headers.hasMoreElements()) {
            Header header = headers.nextElement();
            HeaderUtil.setHeaderToEntity(entity, header.getName(), header.getValue());
        }
    }

    private static BArray getArrayOfEntities(ArrayList<BObject> entities) {
        Type typeOfEntity = TypeUtils.getType(entities.get(0));
        BObject[] result = entities.toArray(new BObject[entities.size()]);
        return ValueCreator.createArrayValue(result, TypeCreator.createArrayType(typeOfEntity));
    }

    private static BObject createMediaTypeObject() {
        return ValueCreator.createObjectValue(MimeUtil.getMimePackage(), MEDIA_TYPE);
    }

    private static BObject createEntityObject() {
        return ValueCreator.createObjectValue(MimeUtil.getMimePackage(), ENTITY);
    }

    private static String extractFromAddressFromMessage(Message message) throws MessagingException {
        String fromAddress = "";
        if (message.getFrom() != null) {
            fromAddress = message.getFrom()[0].toString();
        }
        return fromAddress;
    }

    private static String getSenderAddress(Message message) throws MessagingException {
        String senderAddress = "";
        if (message instanceof POP3Message) {
            if (((POP3Message) message).getSender() != null) {
                senderAddress = ((POP3Message) message).getSender().toString();
            }
        } else if (message instanceof IMAPMessage) {
            if (((IMAPMessage) message).getSender() != null) {
                senderAddress = ((IMAPMessage) message).getSender().toString();
            }
        }
        return senderAddress;
    }

    private static Object getAddressBArrayList(Address[] addresses) {
        BArray addressArrayValue = ValueCreator.createArrayValue(stringArrayType);
        if (addresses != null) {
            if (addresses.length > 1) {
                for (Address address: addresses) {
                    addressArrayValue.append(StringUtils.fromString(address.toString()));
                }
            } else {
                return addresses[0].toString();
            }

        }
        return addressArrayValue;
    }

    private static String getStringNullChecked(String string) {
        return string == null ? "" : string;
    }

    @ExcludeCoverageFromGeneratedReport
    private static void printPopDebugLogs(Properties properties) {
        if (log.isDebugEnabled()) {
            Set<String> propertySet = properties.stringPropertyNames();
            log.debug("POP3 Properties set are as follows.");
            for (Object propertyObj : propertySet) {
                log.debug("Property Name: " + propertyObj + ", Value: " + properties.get(propertyObj).toString()
                        + " ValueType: " + properties.get(propertyObj).getClass().getName());
            }
        }
    }

    @ExcludeCoverageFromGeneratedReport
    private static void printImapDebugLogs(Properties properties) {
        if (log.isDebugEnabled()) {
            Set<String> propertySet = properties.stringPropertyNames();
            log.debug("IMAP4 Properties set are as follows.");
            for (Object propertyObj : propertySet) {
                log.debug("Property Name: " + propertyObj + ", Value: " + properties.get(propertyObj).toString()
                        + " ValueType: " + properties.get(propertyObj).getClass().getName());
            }
        }
    }

}
