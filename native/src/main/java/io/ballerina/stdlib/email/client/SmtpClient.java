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

import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.email.util.CommonUtil;
import io.ballerina.stdlib.email.util.EmailConstants;
import io.ballerina.stdlib.email.util.SmtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * Contains functionality of SMTP Client.
 *
 * @since 1.2.0
 */
public class SmtpClient {

    private static final Logger log = LoggerFactory.getLogger(SmtpClient.class);

    private SmtpClient() {
    }

    /**
     * Initializes the BObject object with the SMTP Properties.
     *
     * @param clientEndpoint Represents the SMTP Client class
     * @param host           Represents the host address of the SMTP server
     * @param username       Represents the username of the SMTP server
     * @param password       Represents the password of the SMTP server
     * @param config         Properties required to configure the SMTP Session
     * @return If an error occurs in the SMTP client, error
     */
    public static Object initClientEndpoint(BObject clientEndpoint, BString host, BString username, BString password,
                                            BMap<BString, Object> config) {
        Properties properties = getPropertiesFromConfig(host.getValue(), config, true);
        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username.getValue(), password.getValue());
                    }
                });
        clientEndpoint.addNativeData(EmailConstants.PROPS_SESSION, session);
        clientEndpoint.addNativeData(EmailConstants.PROPS_USERNAME.getValue(), username.getValue());
        // Store configuration for enhanced error reporting
        clientEndpoint.addNativeData(EmailConstants.PROPS_HOST.getValue(), host.getValue());
        clientEndpoint.addNativeData(EmailConstants.PROPS_PORT.getValue(), config.getIntValue(EmailConstants.PROPS_PORT));
        BString security = config.getStringValue(EmailConstants.PROPS_SECURITY);
        clientEndpoint.addNativeData(EmailConstants.PROPS_SECURITY.getValue(), 
                security != null ? security.getValue() : null);
        return null;
    }

    public static Object initNoAuthSmtpClientEndpoint(BObject clientEndpoint, BString host,
                                                      BMap<BString, Object> config) {
        Properties properties = getPropertiesFromConfig(host.getValue(), config, false);
        Session session = Session.getInstance(properties);
        clientEndpoint.addNativeData(EmailConstants.PROPS_SESSION, session);
        // Store configuration for enhanced error reporting
        clientEndpoint.addNativeData(EmailConstants.PROPS_HOST.getValue(), host.getValue());
        clientEndpoint.addNativeData(EmailConstants.PROPS_PORT.getValue(), config.getIntValue(EmailConstants.PROPS_PORT));
        BString security = config.getStringValue(EmailConstants.PROPS_SECURITY);
        clientEndpoint.addNativeData(EmailConstants.PROPS_SECURITY.getValue(), 
                security != null ? security.getValue() : null);
        return null;
    }

    /**
     * Sends an email to an SMTP server.
     *
     * @param clientConnector Represents the SMTP Client class
     * @param message         Fields of an email
     * @return If an error occurs in the SMTP client, error
     */
    public static Object sendMessage(BObject clientConnector, BMap<BString, Object> message) {
        try {
            Session session = (Session) clientConnector.getNativeData(EmailConstants.PROPS_SESSION);
            String username = (String) clientConnector.getNativeData(EmailConstants.PROPS_USERNAME.getValue());
            Transport.send(SmtpUtil.generateMessage(session, username, message));
            return null;
        } catch (BError e) {
            return e;
        } catch (SendFailedException e) {
            String invalidAddresses = Arrays.stream(e.getInvalidAddresses())
                    .map((Address::toString))
                    .collect(Collectors.joining(","));
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Error while sending the message to SMTP server : " + e.getMessage() + " " + invalidAddresses);
        } catch (MessagingException | IOException e) {
            log.debug("Error while sending the message to SMTP server : ", e);
            
            // Enhance connection-related errors with specific diagnostic information
            if (isConnectionError(e)) {
                String host = (String) clientConnector.getNativeData(EmailConstants.PROPS_HOST.getValue());
                Long portLong = (Long) clientConnector.getNativeData(EmailConstants.PROPS_PORT.getValue());
                int port = portLong != null ? portLong.intValue() : 25;
                String security = (String) clientConnector.getNativeData(EmailConstants.PROPS_SECURITY.getValue());
                
                String enhancedMessage = CommonUtil.createEnhancedSmtpErrorMessage(e, host, port, security);
                return CommonUtil.getBallerinaError(EmailConstants.ERROR, enhancedMessage);
            }
            
            // For non-connection errors, preserve original message for compatibility
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
    }
    
    /**
     * Efficiently determines if the exception indicates an SMTP connection failure.
     * Focuses on the most common connection error patterns.
     *
     * @param e The exception to analyze
     * @return true if the exception indicates a connection problem
     */
    private static boolean isConnectionError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        String lowerMessage = message.toLowerCase();
        // Check for most common connection error patterns first (performance optimization)
        return lowerMessage.contains("could not connect") ||
               lowerMessage.contains("connection refused") ||
               lowerMessage.contains("connection timed out") ||
               lowerMessage.contains("connect timed out") ||
               lowerMessage.contains("timed out") ||
               lowerMessage.contains("network is unreachable") ||
               lowerMessage.contains("no route to host") ||
               lowerMessage.contains("connection reset") ||
               lowerMessage.contains("starttls") ||
               lowerMessage.contains("ssl") ||
               lowerMessage.contains("tls");
    }

    private static Properties getPropertiesFromConfig(String host, BMap<BString, Object> config, boolean requireAuth) {
        if (config.isEmpty()) {
            throw CommonUtil.getBallerinaError(EmailConstants.ERROR, "SmtpConfiguration should not be empty.");
        }
        try {
            return SmtpUtil.getProperties(config, host, requireAuth);
        } catch (IOException | GeneralSecurityException e) {
            log.debug("Error while initializing SMTP properties : ", e);
            throw CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
    }
}
