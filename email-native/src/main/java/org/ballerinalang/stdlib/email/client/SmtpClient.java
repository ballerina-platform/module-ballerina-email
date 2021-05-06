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

package org.ballerinalang.stdlib.email.client;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.stdlib.email.util.CommonUtil;
import org.ballerinalang.stdlib.email.util.EmailConstants;
import org.ballerinalang.stdlib.email.util.SmtpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

import static org.ballerinalang.stdlib.email.util.EmailConstants.HOSTNAME_CHECKER_CLASS;

/**
 * Contains functionality of SMTP Client.
 *
 * @since 1.2.0
 */
public class SmtpClient {

    private static final Logger log = LoggerFactory.getLogger(SmtpClient.class);

    private SmtpClient() {}

    static {
        Class<?> hostNameCheckerClass = null;
        try {
            hostNameCheckerClass = Class.forName(HOSTNAME_CHECKER_CLASS);
        } catch (ClassNotFoundException e) {
            log.error("sun.security.util.HostnameChecker class was not found from SmtpClient.");
        }
        if (hostNameCheckerClass != null) {
            String hostNameCheckerPackageName = hostNameCheckerClass.getPackageName();
            Module smtpClientModule = SmtpClient.class.getModule();
            hostNameCheckerClass.getModule().addOpens(hostNameCheckerPackageName, smtpClientModule);
        }
    }

    /**
     * Initializes the BObject object with the SMTP Properties.
     * @param clientEndpoint Represents the SMTP Client class
     * @param host Represents the host address of the SMTP server
     * @param username Represents the username of the SMTP server
     * @param password Represents the password of the SMTP server
     * @param config Properties required to configure the SMTP Session
     * @return If an error occurs in the SMTP client, error
     */
    public static Object initClientEndpoint(BObject clientEndpoint, BString host, BString username, BString password,
                                          BMap<BString, Object> config) {
        if (config.size() == 0) {
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, "SmtpConfiguration should not be empty.");
        }
        Properties properties;
        try {
            properties = SmtpUtil.getProperties(config, host.getValue());
        } catch (IOException | GeneralSecurityException e) {
            log.error("Failed to initialize SMTP server : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
        Session session = Session.getInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username.getValue(), password.getValue());
                    }
                });
        clientEndpoint.addNativeData(EmailConstants.PROPS_SESSION, session);
        clientEndpoint.addNativeData(EmailConstants.PROPS_USERNAME.getValue(), username.getValue());
        return null;
    }

    /**
     * Sends an email to an SMTP server.
     * @param clientConnector Represents the SMTP Client class
     * @param message Fields of an email
     * @return If an error occurs in the SMTP client, error
     */
    public static Object sendMessage(BObject clientConnector, BMap<BString, Object> message) {
        try {
            Transport.send(SmtpUtil.generateMessage(
                    (Session) clientConnector.getNativeData(EmailConstants.PROPS_SESSION),
                    (String) clientConnector.getNativeData(EmailConstants.PROPS_USERNAME.getValue()), message));
            return null;
        } catch (MessagingException | IOException e) {
            log.error("Failed to send message to SMTP server : ", e);
            return CommonUtil.getBallerinaError(EmailConstants.ERROR, e.getMessage());
        }
    }

}
