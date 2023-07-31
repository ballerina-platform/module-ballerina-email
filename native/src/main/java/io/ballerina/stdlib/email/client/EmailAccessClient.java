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

package io.ballerina.stdlib.email.client;

import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.email.util.CommonUtil;
import io.ballerina.stdlib.email.util.EmailAccessUtil;
import io.ballerina.stdlib.email.util.EmailConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.FlagTerm;

/**
 * Contains the functionality of email reading with POP and IMAP clients.
 *
 * @since 1.2.0
 */
public class EmailAccessClient {

    private static final Logger log = LoggerFactory.getLogger(EmailAccessClient.class);
    private static final FlagTerm UNSEEN_FLAG = new FlagTerm(new Flags(Flags.Flag.SEEN), false);

    private EmailAccessClient() {}

    /**
     * Initializes the BObject object with the POP properties.
     * @param clientEndpoint Represents the POP Client class
     * @param host Represents the host address of POP server
     * @param username Represents the username of the POP server
     * @param password Represents the password of the POP server
     * @param config Properties required to configure the POP session
     * @return If an error occurs in the POP client, returns an error
     */
    public static Object initPopClientEndpoint(BObject clientEndpoint, BString host, BString username, BString password,
                                               BMap<BString, Object> config) {
        Properties properties;
        try {
            properties = EmailAccessUtil.getPopProperties(config, host.getValue());
        } catch (IOException | GeneralSecurityException e) {
            log.debug("Error while initializing POP3 client properties : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while initializing POP3 client properties: " + e.getMessage());
        }
        Session session = Session.getInstance(properties, null);
        try {
            Store store = session.getStore(EmailConstants.POP_PROTOCOL);
            store.connect(host.getValue(), username.getValue(), password.getValue());
            clientEndpoint.addNativeData(EmailConstants.PROPS_STORE, store);
            clientEndpoint.addNativeData(EmailConstants.PROPS_HOST.getValue(), host.getValue());
            clientEndpoint.addNativeData(EmailConstants.PROPS_USERNAME.getValue(), username.getValue());
            clientEndpoint.addNativeData(EmailConstants.PROPS_PASSWORD.getValue(), password.getValue());
        } catch (MessagingException e) {
            log.debug("Error while connecting to the POP3 store : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while connecting to the POP3 store: " + e.getMessage());
        }
        return null;
    }

    /**
     * Initializes the BObject object with the IMAP properties.
     * @param clientEndpoint Represents the IMAP Client class
     * @param host Represents the host address of IMAP server
     * @param username Represents the username of the IMAP server
     * @param password Represents the password of the IMAP server
     * @param config Properties required to configure the IMAP session
     * @return If an error occurs in the IMAP client, returns an error
     */
    public static Object initImapClientEndpoint(BObject clientEndpoint, BString host, BString username,
                                                BString password, BMap<BString, Object> config) {
        Properties properties;
        try {
            properties = EmailAccessUtil.getImapProperties(config, host.getValue());
        } catch (IOException | GeneralSecurityException e) {
            log.debug("Error while initializing IMAP client properties : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while initializing IMAP client properties: " + e.getMessage());
        }
        Session session = Session.getInstance(properties, null);
        try {
            Store store = session.getStore(EmailConstants.IMAP_PROTOCOL);
            store.connect(host.getValue(), username.getValue(), password.getValue());
            clientEndpoint.addNativeData(EmailConstants.PROPS_STORE, store);
            clientEndpoint.addNativeData(EmailConstants.PROPS_HOST.getValue(), host.getValue());
            clientEndpoint.addNativeData(EmailConstants.PROPS_USERNAME.getValue(), username.getValue());
            clientEndpoint.addNativeData(EmailConstants.PROPS_PASSWORD.getValue(), password.getValue());
        } catch (MessagingException e) {
            log.debug("Error while connecting to the IMAP store : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while connecting to the IMAP store: " + e.getMessage());
        }
        return null;
    }

    /**
     * Read emails from the server.
     * @param clientConnector Represents the POP or IMAP client class
     * @param folderName Name of the folder to read emails
     * @param timeout Timeout interval in seconds
     * @return If successful return the received email, otherwise an error
     */
    public static Object readMessage(BObject clientConnector, BString folderName, BDecimal timeout) {
        if (timeout.intValue() == 0) {
            return readMessageFromFolder(clientConnector, folderName);
        }
        BDecimal polling = BDecimal.valueOf(100);
        double pollingInterval = polling.floatValue();
        double timeoutInterval = timeout.floatValue();
        int timeoutIntervalInMs = (int) (timeoutInterval * 1000);
        int pollingIntervalInMs = (int) (pollingInterval * 1000);
        // Minimum polling interval is set to 1 ms to avoid dividing by zero
        if (pollingIntervalInMs == 0) {
            pollingIntervalInMs = 1;
        }
        // maxPollingCount is the maximum number of polls possible when the email read operation time is negligible
        int maxPollingCount = timeoutIntervalInMs / pollingIntervalInMs + 1;
        long startTimeInMs = System.currentTimeMillis();
        long endTimeInMs = startTimeInMs + timeoutIntervalInMs;
        long estimatedLastReadTimeInMs;

        Object message = readMessageFromFolder(clientConnector, folderName);
        if (message != null) {
            return message;
        }
        try {
            for (int i = 0; i < maxPollingCount; i++) {
                long currentTimeInMs = System.currentTimeMillis();
                long elapsedTimeInMs = currentTimeInMs - startTimeInMs;
                long remainingTimeInMs = endTimeInMs - currentTimeInMs;
                if (remainingTimeInMs > 0) {
                    if ((i < maxPollingCount - 1) && (remainingTimeInMs > pollingIntervalInMs)) {
                        Thread.sleep(pollingIntervalInMs);
                    } else {
                        // Take the average of past read operation times as the estimate
                        estimatedLastReadTimeInMs = (elapsedTimeInMs - i * pollingIntervalInMs) / (i + 1);
                        long lastSleepTimeInMs = remainingTimeInMs - estimatedLastReadTimeInMs;
                        if (lastSleepTimeInMs > 0) {
                            Thread.sleep(lastSleepTimeInMs);
                        } else {
                            break;
                        }
                    }
                    message = readMessageFromFolder(clientConnector, folderName);
                    if (message != null) {
                        return message;
                    }
                } else {
                    break;
                }
            }
        } catch (InterruptedException e) {
            log.error("Interrupted during the thread sleep : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
        return null;
    }

    private static Object readMessageFromFolder(BObject clientConnector, BString folderName) {
        BMap<BString, Object> mapValue = null;
        try {
            Store store = (Store) clientConnector.getNativeData(EmailConstants.PROPS_STORE);
            Object folderObj = clientConnector.getNativeData(EmailConstants.PROPS_FOLDER);
            if (folderObj instanceof Folder && ((Folder) folderObj).isOpen()) {
                ((Folder) folderObj).close();
            }
            Folder folder = store.getFolder(folderName.getValue());
            if (folder == null) {
                log.error("Email store folder, " + folderName + " is not found.");
            } else {
                if (!folder.isOpen()) {
                    folder.open(Folder.READ_WRITE);
                }
                clientConnector.addNativeData(EmailConstants.PROPS_FOLDER, folder);
                Message[] messages = folder.search(UNSEEN_FLAG);
                if (messages.length > 0) {
                    mapValue = EmailAccessUtil.getMapValue(messages[0]);
                    Flags flags = new Flags();
                    if (EmailConstants.POP_CLIENT.equals(TypeUtils.getType(clientConnector).getName())) {
                        flags.add(Flags.Flag.DELETED);
                    } else {
                        flags.add(Flags.Flag.SEEN);
                    }
                    folder.setFlags(new int[]{messages[0].getMessageNumber()}, flags, true);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Got the messages. Email count = " + messages.length);
                }
            }
            return mapValue;
        } catch (MessagingException | IOException e) {
            log.debug("Error while email folder operation : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
    }

    public static Object close(BObject clientConnector) {
        try {
            Store store = (Store) clientConnector.getNativeData(EmailConstants.PROPS_STORE);
            Folder folder = (Folder) clientConnector.getNativeData(EmailConstants.PROPS_FOLDER);
            if (folder.isOpen()) {
                folder.close(false);
            }
            store.close();
        } catch (MessagingException e) {
            log.debug("Error while closing the client : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
        return null;
    }

}
