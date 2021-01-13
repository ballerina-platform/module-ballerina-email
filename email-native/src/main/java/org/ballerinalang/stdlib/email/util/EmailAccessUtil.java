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

import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.pop3.POP3Message;
import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.creators.TypeCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BXml;
import io.ballerina.runtime.api.values.BXmlSequence;
import org.ballerinalang.mime.util.EntityBodyChannel;
import org.ballerinalang.mime.util.EntityBodyHandler;
import org.ballerinalang.mime.util.EntityWrapper;
import org.ballerinalang.mime.util.HeaderUtil;
import org.ballerinalang.mime.util.MimeConstants;
import org.ballerinalang.mime.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import static org.ballerinalang.mime.util.MimeConstants.BODY_PARTS;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY;
import static org.ballerinalang.mime.util.MimeConstants.ENTITY_BYTE_CHANNEL;
import static org.ballerinalang.mime.util.MimeConstants.MEDIA_TYPE;
import static org.ballerinalang.mime.util.MimeConstants.OCTET_STREAM;
import static org.ballerinalang.mime.util.MimeConstants.PROTOCOL_MIME_PKG_ID;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_START_TLS_ALWAYS;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_START_TLS_AUTO;
import static org.ballerinalang.stdlib.email.util.EmailConstants.PROPS_START_TLS_NEVER;

/**
 * Contains utility functions related to the POP and IMAP protocols.
 *
 * @since 1.2.0
 */
public class EmailAccessUtil {

    private static final Logger log = LoggerFactory.getLogger(EmailAccessUtil.class);
    private static final ArrayType stringArrayType = TypeCreator.createArrayType(PredefinedTypes.TYPE_STRING);

    /**
     * Generates Properties object using the passed BMap.
     *
     * @param emailAccessConfig BMap with the configuration values
     * @param host Host address of email server
     * @return Properties Email server access properties
     */
    public static Properties getPopProperties(BMap<BString, Object> emailAccessConfig, String host) {
        Properties properties = new Properties();
        properties.put(EmailConstants.PROPS_POP_HOST, host);
        properties.put(EmailConstants.PROPS_POP_PORT,
                Long.toString(emailAccessConfig.getIntValue(EmailConstants.PROPS_PORT)));
        BString security = emailAccessConfig.getStringValue(EmailConstants.PROPS_SECURITY);
        if (security != null) {
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
                    break;
                case PROPS_START_TLS_NEVER:
                    properties.put(EmailConstants.PROPS_POP_STARTTLS, "false");
                    properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "false");
                    break;
                default:
                    properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "true");
            }
        } else {
            properties.put(EmailConstants.PROPS_POP_SSL_ENABLE, "true");
        }
        properties.put(EmailConstants.PROPS_POP_AUTH, "true");
        properties.put(EmailConstants.MAIL_STORE_PROTOCOL, EmailConstants.POP_PROTOCOL);
        CommonUtil.addCustomProperties(
                (BMap<BString, Object>) emailAccessConfig.getMapValue(EmailConstants.PROPS_PROPERTIES), properties);
        if (log.isDebugEnabled()) {
            Set<String> propertySet = properties.stringPropertyNames();
            log.debug("POP3 Properties set are as follows.");
            for (Object propertyObj : propertySet) {
                log.debug("Property Name: " + propertyObj + ", Value: " + properties.get(propertyObj).toString()
                        + " ValueType: " + properties.get(propertyObj).getClass().getName());
            }
        }
        return properties;
    }

    /**
     * Generates Properties object using the passed BMap.
     *
     * @param emailAccessConfig BMap with the configuration values
     * @param host Host address of email server
     * @return Properties Email server access properties
     */
    public static Properties getImapProperties(BMap<BString, Object> emailAccessConfig, String host) {
        Properties properties = new Properties();
        properties.put(EmailConstants.PROPS_IMAP_HOST, host);
        properties.put(EmailConstants.PROPS_IMAP_PORT,
                Long.toString(emailAccessConfig.getIntValue(EmailConstants.PROPS_PORT)));
        BString security = emailAccessConfig.getStringValue(EmailConstants.PROPS_SECURITY);
        if (security != null) {
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
                    break;
                case PROPS_START_TLS_NEVER:
                    properties.put(EmailConstants.PROPS_IMAP_STARTTLS, "false");
                    properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "false");
                    break;
                default:
                    properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "true");
            }
        } else {
            properties.put(EmailConstants.PROPS_IMAP_SSL_ENABLE, "true");
        }
        properties.put(EmailConstants.PROPS_IMAP_AUTH, "true");
        properties.put(EmailConstants.MAIL_STORE_PROTOCOL, EmailConstants.IMAP_PROTOCOL);
        CommonUtil.addCustomProperties(
                (BMap<BString, Object>) emailAccessConfig.getMapValue(EmailConstants.PROPS_PROPERTIES), properties);
        if (log.isDebugEnabled()) {
            Set<String> propertySet = properties.stringPropertyNames();
            log.debug("IMAP4 Properties set are as follows.");
            for (Object propertyObj : propertySet) {
                log.debug("Property Name: " + propertyObj + ", Value: " + properties.get(propertyObj).toString()
                        + " ValueType: " + properties.get(propertyObj).getClass().getName());
            }
        }
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

    private static BMap<BString, Object> extractHeadersFromMessage(Message message) throws MessagingException {
        BMap<BString, Object> headerMap = ValueCreator.createMapValue();
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
        String messageBody = "";
        if (message.getContentType() != null && CommonUtil.isTextBased(message.getContentType())) {
            if (message.getContent() != null) {
                messageBody = message.getContent().toString();
            }
        } else if (message.isMimeType(EmailConstants.MIME_CONTENT_TYPE_PATTERN)) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            if (mimeMultipart != null && mimeMultipart.getCount() > 0 && mimeMultipart.getBodyPart(0) != null
                    && mimeMultipart.getBodyPart(0).getContent() != null) {
                Object messageObject = mimeMultipart.getBodyPart(0).getContent();
                if (messageObject instanceof String) {
                    messageBody = (String) messageObject;
                }
            }
        }
        return messageBody;
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
            if (contentType != null && bodyPart.getContent() instanceof String) {
                if (CommonUtil.isJsonBased(contentType)) {
                    entityArray.add(getJsonEntity(bodyPart));
                } else if (CommonUtil.isXmlBased(contentType)) {
                    entityArray.add(getXmlEntity(bodyPart));
                } else {
                    entityArray.add(getTextEntity(bodyPart));
                }
            } else {
                entityArray.add(getBinaryEntity(bodyPart));
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
                attachMultipart(bodyPart, entityArray);
            }
            return entityArray;
        } else {
            return null;
        }
    }

    private static BObject getJsonEntity(BodyPart bodyPart) throws IOException, MessagingException {
        String jsonContent = (String) bodyPart.getContent();
        BObject entity = createEntityObject();
        EntityWrapper byteChannel = EntityBodyHandler.getEntityWrapper(jsonContent);
        entity.addNativeData(MimeConstants.ENTITY_BYTE_CHANNEL, byteChannel);
        MimeUtil.setContentType(createMediaTypeObject(), entity, MimeConstants.APPLICATION_JSON);
        setEntityHeaders(entity, bodyPart);
        return entity;
    }

    private static BObject getXmlEntity(BodyPart bodyPart) throws IOException, MessagingException {
        String xmlContent = (String) bodyPart.getContent();
        BXml xmlNode = (BXml) XmlUtils.parse(xmlContent);
        BObject entity = createEntityObject();
        EntityBodyChannel byteChannel = new EntityBodyChannel(new ByteArrayInputStream(
                xmlNode.stringValue(null).getBytes(StandardCharsets.UTF_8)));
        entity.addNativeData(ENTITY_BYTE_CHANNEL, new EntityWrapper(byteChannel));
        MimeUtil.setContentType(createMediaTypeObject(), entity, MimeConstants.APPLICATION_XML);
        setEntityHeaders(entity, bodyPart);
        return entity;
    }

    private static BObject getTextEntity(BodyPart bodyPart) throws IOException, MessagingException {
        String textPayload = (String) bodyPart.getContent();
        BObject entity = ValueCreator.createObjectValue(PROTOCOL_MIME_PKG_ID, ENTITY);
        entity.addNativeData(ENTITY_BYTE_CHANNEL, EntityBodyHandler.getEntityWrapper(textPayload));
        MimeUtil.setContentType(createMediaTypeObject(), entity, MimeConstants.TEXT_PLAIN);
        setEntityHeaders(entity, bodyPart);
        return entity;
    }

    private static BObject getBinaryEntity(BodyPart bodyPart) throws IOException, MessagingException {
        byte[] binaryContent = CommonUtil.convertInputStreamToByteArray(bodyPart.getInputStream());
        EntityWrapper byteChannel = new EntityWrapper(new EntityBodyChannel(new ByteArrayInputStream(binaryContent)));
        BObject entity = createEntityObject();
        entity.addNativeData(ENTITY_BYTE_CHANNEL, byteChannel);
        MimeUtil.setContentType(createMediaTypeObject(), entity, OCTET_STREAM);
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
        Type typeOfEntity = entities.get(0).getType();
        BObject[] result = entities.toArray(new BObject[entities.size()]);
        return ValueCreator.createArrayValue(result, TypeCreator.createArrayType(typeOfEntity));
    }

    private static BObject createMediaTypeObject() {
        return ValueCreator.createObjectValue(PROTOCOL_MIME_PKG_ID, MEDIA_TYPE);
    }

    private static BObject createEntityObject() {
        return ValueCreator.createObjectValue(PROTOCOL_MIME_PKG_ID, ENTITY);
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

}
