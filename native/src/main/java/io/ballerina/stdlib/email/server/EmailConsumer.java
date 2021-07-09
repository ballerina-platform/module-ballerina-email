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

package io.ballerina.stdlib.email.server;

import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.email.client.EmailAccessClient;
import io.ballerina.stdlib.email.util.EmailConstants;
import io.ballerina.stdlib.email.util.EmailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Provides the capability to read an email and forward it to a listener.
 *
 * @since 1.3.0
 */
public class EmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailConsumer.class);

    private EmailListener emailListener;
    private BObject client;

    /**
     * Constructor for the EmailConsumer.
     *
     * @param emailProperties Map of property values
     * @param listener Forwards the received emails to Ballerina code
     * @throws EmailConnectorException If the given protocol is invalid
     */
    public EmailConsumer(Map<String, Object> emailProperties, EmailListener listener) throws EmailConnectorException {
        this.emailListener = listener;
        String host = (String) emailProperties.get(EmailConstants.PROPS_HOST.getValue());
        String username = (String) emailProperties.get(EmailConstants.PROPS_USERNAME.getValue());
        String password = (String) emailProperties.get(EmailConstants.PROPS_PASSWORD.getValue());
        String protocol = (String) emailProperties.get(EmailConstants.PROPS_PROTOCOL.getValue());
        BMap<BString, Object> protocolConfig = (BMap<BString, Object>) emailProperties.get(
                EmailConstants.PROTOCOL_CONFIG.getValue());
        if (protocol.equals(EmailConstants.IMAP)) {
            client = ValueCreator.createObjectValue(EmailUtils.getEmailPackage(), EmailConstants.IMAP_CLIENT,
                                                       StringUtils.fromString(host), StringUtils.fromString(username),
                                                       StringUtils.fromString(password), protocolConfig);
            EmailAccessClient.initImapClientEndpoint(client, StringUtils.fromString(host),
                                                     StringUtils.fromString(username),
                                                     StringUtils.fromString(password), protocolConfig);
        } else if (protocol.equals(EmailConstants.POP)) {
            client = ValueCreator.createObjectValue(EmailUtils.getEmailPackage(), EmailConstants.POP_CLIENT,
                                                       StringUtils.fromString(host), StringUtils.fromString(username),
                                                       StringUtils.fromString(password), protocolConfig);
            EmailAccessClient.initPopClientEndpoint(client, StringUtils.fromString(host),
                                                    StringUtils.fromString(username),
                                                    StringUtils.fromString(password), protocolConfig);
        } else {
            String errorMsg = "Protocol should either be 'IMAP' or 'POP'.";
            throw new EmailConnectorException(errorMsg);
        }
    }

    /**
     * Read emails from the Email client and pass to the listener.
     */
    public void consume() {
        if (log.isDebugEnabled()) {
            log.debug("Consumer thread name: " + Thread.currentThread().getName());
            log.debug("Consumer hashcode: " + this.hashCode());
            log.debug("Polling for an email...");
        }
        Object message = EmailAccessClient.readMessage(client,
                StringUtils.fromString(EmailConstants.DEFAULT_STORE_LOCATION), BDecimal.valueOf(0));
        if (message != null) {
            if (message instanceof BMap) {
                emailListener.onMessage(new EmailEvent(message));
            } else if (message instanceof BError) {
                emailListener.onError(message);
            } else {
                emailListener.onError(ErrorCreator.createError(
                        new EmailConnectorException("Received an undefined message from email server.")));
            }
        } else {
            log.debug("No emails found in the inbox.");
        }

    }

    /**
     * Close email polling job from the Email client and pass to the listener.
     */
    public void close() {
        if (log.isDebugEnabled()) {
            log.debug("Close thread name: " + Thread.currentThread().getName());
            log.debug("Close hashcode: " + this.hashCode());
            log.debug("Polling for closing...");
        }
        Object message = EmailAccessClient.close(client);
        if (message instanceof BError) {
            emailListener.onClose(message);
        } else {
            emailListener.onClose(null);
        }

    }

    protected EmailListener getEmailListener() {
        return emailListener;
    }

}
