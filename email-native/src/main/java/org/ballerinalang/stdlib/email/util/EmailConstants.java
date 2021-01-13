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

import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.runtime.api.constants.RuntimeConstants.BALLERINA_BUILTIN_PKG_PREFIX;

/**
 * Constants of the Email module.
 *
 * @since 1.2.0
 */
public class EmailConstants {

    // Common constants
    public static final String MODULE_NAME = "email";

     /**
     * email standard library version.
     * @deprecated Use EmailUtils.getEmailPackage().getVersion().
     */
    @Deprecated
    public static final String MODULE_VERSION = "1.0.4";

     /**
     * email standard library package ID.
     * @deprecated Use EmailUtils.getEmailPackage().
     */
    @Deprecated
    public static final Module EMAIL_PACKAGE_ID = new Module(BALLERINA_BUILTIN_PKG_PREFIX, MODULE_NAME,
                                                                 MODULE_VERSION);
    public static final String HTML_CONTENT_TYPE = "text/html";
    public static final BString ATTACHMENT_FILE_PATH = StringUtils.fromString("filePath");
    public static final BString ATTACHMENT_CONTENT_TYPE = StringUtils.fromString("contentType");
    public static final BString PROPS_PORT = StringUtils.fromString("port");
    public static final BString PROPS_USERNAME = StringUtils.fromString("username");
    public static final BString MESSAGE_TO = StringUtils.fromString("to");
    public static final BString MESSAGE_CC = StringUtils.fromString("cc");
    public static final BString MESSAGE_BCC = StringUtils.fromString("bcc");
    public static final BString MESSAGE_SUBJECT = StringUtils.fromString("subject");
    public static final BString MESSAGE_MESSAGE_BODY = StringUtils.fromString("body");
    public static final BString MESSAGE_HTML_MESSAGE_BODY = StringUtils.fromString("htmlBody");
    public static final BString MESSAGE_BODY_CONTENT_TYPE = StringUtils.fromString("contentType");
    public static final BString MESSAGE_FROM = StringUtils.fromString("from");
    public static final BString MESSAGE_SENDER = StringUtils.fromString("sender");
    public static final BString MESSAGE_REPLY_TO = StringUtils.fromString("replyTo");
    public static final BString MESSAGE_ATTACHMENTS = StringUtils.fromString("attachments");
    public static final BString MESSAGE_HEADERS = StringUtils.fromString("headers");

    // Common constants to POP and IMAP
    public static final BString PROPS_PROPERTIES = StringUtils.fromString("properties");
    public static final BString PROPS_SSL = StringUtils.fromString("enableSsl");
    public static final BString PROPS_SECURITY = StringUtils.fromString("security");
    public static final BString PROPS_HOST = StringUtils.fromString("host");
    public static final BString PROPS_PASSWORD = StringUtils.fromString("password");
    public static final BString PROPS_PROTOCOL = StringUtils.fromString("protocol");
    public static final String PROPS_START_TLS_AUTO = "START_TLS_AUTO";
    public static final String PROPS_START_TLS_ALWAYS = "START_TLS_ALWAYS";
    public static final String PROPS_START_TLS_NEVER = "START_TLS_NEVER";
    public static final String PROPS_SSL_ALWAYS = "SSL";

    public static final String PROPS_STORE = "store";
    public static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";
    public static final String MIME_CONTENT_TYPE_PATTERN = "multipart/*";
    public static final String READ_CLIENT_INIT_ERROR = "ReadClientInitError";
    public static final String READ_ERROR = "ReadError";
    public static final BString ENDPOINT_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString ENDPOINT_CONFIG_PRIVATE_KEY = StringUtils.fromString("privateKey");
    public static final BString ENDPOINT_CONFIG_PATH = StringUtils.fromString("path");
    public static final BString ENDPOINT_CONFIG_PASS_KEY = StringUtils.fromString("password");
    public static final String IDENTITY = "IDENTITY";
    public static final String IDENTITY_PASS_PHRASE = "IDENTITY_PASS_PHRASE";
    public static final String URI = "uri";
    public static final String EMAIL_SERVER_CONNECTOR = "serverConnector";
    public static final BString PROTOCOL_CONFIG = StringUtils.fromString("protocolConfig");
    public static final String DEFAULT_STORE_LOCATION = "INBOX";
    public static final String ON_EMAIL_MESSAGE = "onEmailMessage";
    public static final String ON_ERROR = "onError";
    public static final String LISTENER = "Listener";

    // POP related constants
    public static final String POP = "POP";
    public static final String POP_CLIENT = "PopClient";
    public static final String POP_PROTOCOL = "pop3";
    public static final String PROPS_POP_HOST = "mail.pop3.host";
    public static final String PROPS_POP_PORT = "mail.pop3.port";
    public static final String PROPS_POP_AUTH = "mail.pop.auth";
    public static final String PROPS_POP_STARTTLS = "mail.pop3.starttls.enable";
    public static final String PROPS_POP_STARTTLS_REQUIRED = "mail.pop3.starttls.required";
    public static final String PROPS_POP_SSL_ENABLE = "mail.pop3.ssl.enable";

    // IMAP related constants
    public static final String IMAP = "IMAP";
    public static final String IMAP_CLIENT = "ImapClient";
    public static final String IMAP_PROTOCOL = "imap";
    public static final String PROPS_IMAP_HOST = "mail.imap.host";
    public static final String PROPS_IMAP_PORT = "mail.imap.port";
    public static final String PROPS_IMAP_STARTTLS = "mail.imap.starttls.enable";
    public static final String PROPS_IMAP_SSL_ENABLE = "mail.imap.ssl.enable";
    public static final String PROPS_IMAP_STARTTLS_REQUIRED = "mail.imap.starttls.required";
    public static final String PROPS_IMAP_AUTH = "mail.imap.auth";

    // SMTP related constants
    public static final String PROPS_SESSION = "session";
    public static final String PROPS_ENABLE_SSL = "mail.smtp.ssl.enable";
    public static final String PROPS_SMTP_HOST = "mail.smtp.host";
    public static final String PROPS_SMTP_PORT = "mail.smtp.port";
    public static final String PROPS_SMTP_AUTH = "mail.smtp.auth";
    public static final String PROPS_SMTP_STARTTLS = "mail.smtp.starttls.enable";
    public static final String PROPS_SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";
    public static final String SEND_ERROR = "SendError";
    public static final String INIT_ERROR = "InitializationError";

    public static final String EMAIL_MESSAGE = "Message";
    public static final String ERROR = "Error";

    // Strand meta data
    public static final StrandMetadata ON_MESSAGE_METADATA = new StrandMetadata(BALLERINA_BUILTIN_PKG_PREFIX,
            MODULE_NAME, EmailUtils.getEmailPackage().getVersion(), ON_EMAIL_MESSAGE);

    public static final StrandMetadata ON_ERROR_METADATA = new StrandMetadata(BALLERINA_BUILTIN_PKG_PREFIX,
            MODULE_NAME, EmailUtils.getEmailPackage().getVersion(), ON_ERROR);

    private EmailConstants() {
        // private constructor
    }
}
