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

import com.sun.mail.util.MailSSLSocketFactory;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.stdlib.mime.util.MimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Contains the common utility functions.
 *
 * @since 1.2.1
 */
public class CommonUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);

    private CommonUtil() {}

    /**
     * Check whether the content type is based on text.
     *
     * @param contentType Content Type of a MIME Body Type
     * @return boolean Whether the MIME Body Type is text based
     */
    protected static boolean isTextBased(String contentType) {
        return contentType.startsWith(MimeConstants.TEXT_AS_PRIMARY_TYPE)
                || contentType.endsWith(MimeConstants.XML_SUFFIX)
                || contentType.endsWith(MimeConstants.JSON_SUFFIX)
                || contentType.startsWith(MimeConstants.APPLICATION_JSON)
                || contentType.startsWith(MimeConstants.APPLICATION_XML)
                || contentType.startsWith(MimeConstants.APPLICATION_FORM);
    }

    /**
     * Check whether the content type is based on JSON.
     *
     * @param contentType Content Type of a MIME Body Type
     * @return boolean Whether the MIME Body Type is JSON based
     */
    protected static boolean isJsonBased(String contentType) {
        return Pattern.compile(Pattern.quote(contentType), Pattern.CASE_INSENSITIVE).matcher("json").find();
    }

    /**
     * Check whether the content type is based on XML.
     *
     * @param contentType Content Type of a MIME Body Type
     * @return boolean Whether the MIME Body Type is XML based
     */
    protected static boolean isXmlBased(String contentType) {
        return Pattern.compile(Pattern.quote(contentType), Pattern.CASE_INSENSITIVE).matcher("xml").find();
    }

    /**
     * Convert an InputStream to a byte array.
     *
     * @param inputStream InputStream input
     * @return byte[] Whether the MIME Body Type is text based
     * @throws IOException If an error occurs during reading the InputStream
     */
    public static byte[] convertInputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    protected static SSLSocketFactory createSSLSocketFactory(File crtFile, String protocol)
            throws GeneralSecurityException, IOException {
        SSLContext sslContext = SSLContext.getInstance(protocol);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        X509Certificate result;
        try (InputStream input = new FileInputStream(crtFile)) {
            result = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(input);
        }
        trustStore.setCertificateEntry(crtFile.getName(), result);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    protected static SSLSocketFactory createDefaultSSLSocketFactory()
            throws GeneralSecurityException {
        MailSSLSocketFactory mailSSLSocketFactory = new MailSSLSocketFactory();
        TrustManager[] mailTrustManagers = mailSSLSocketFactory.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance(EmailConstants.DEFAULT_TRANSPORT_PROTOCOL);
        sslContext.init(null, mailTrustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }

    public static BError getBallerinaError(String typeId, String message) {
        return ErrorCreator.createError(ModuleUtils.getModule(), typeId,
                StringUtils.fromString(message), null, null);
    }

    /**
     * Enhances SMTP error messages with specific diagnostic information and actionable recommendations.
     * Provides concise, targeted guidance based on the actual failure mode.
     *
     * @param originalException The original exception thrown
     * @param host             SMTP host that failed to connect
     * @param port             SMTP port that was attempted
     * @param securityConfig   Security configuration used (if any)
     * @return Enhanced error message with specific diagnostic information
     */
    public static String createEnhancedSmtpErrorMessage(Exception originalException, String host, int port, 
                                                       String securityConfig) {
        String originalMsg = originalException.getMessage();
        String lowerMsg = originalMsg != null ? originalMsg.toLowerCase() : "";
        
        // Determine the specific failure type and provide targeted guidance
        SmtpErrorType errorType = categorizeSmtpError(lowerMsg, port, securityConfig);
        
        return String.format("SMTP connection to %s:%d failed: %s. %s", 
                           host, port, originalMsg, errorType.getGuidance());
    }
    
    /**
     * Categorizes SMTP errors into specific types for targeted troubleshooting.
     */
    private static SmtpErrorType categorizeSmtpError(String errorMessage, int port, String security) {
        // Connection refused - server not running or port blocked
        if (errorMessage.contains("connection refused")) {
            if (port == 25) {
                return SmtpErrorType.PORT_25_BLOCKED;
            } else if (port == 465 || port == 587) {
                return SmtpErrorType.CONNECTION_REFUSED;
            }
            return SmtpErrorType.CONNECTION_REFUSED;
        }
        
        // Timeout errors - network or firewall issues
        if (errorMessage.contains("timed out") || errorMessage.contains("timeout")) {
            return SmtpErrorType.CONNECTION_TIMEOUT;
        }
        
        // TLS/SSL related errors
        if (errorMessage.contains("starttls") || errorMessage.contains("tls") || errorMessage.contains("ssl")) {
            if (port == 587 && (security == null || security.equals("none"))) {
                return SmtpErrorType.PORT_587_NEEDS_TLS;
            } else if (port == 465 && (security == null || !security.contains("SSL"))) {
                return SmtpErrorType.PORT_465_NEEDS_SSL;
            }
            return SmtpErrorType.TLS_SSL_ERROR;
        }
        
        // Authentication errors
        if (errorMessage.contains("authentication") || errorMessage.contains("login") || 
            errorMessage.contains("password") || errorMessage.contains("unauthorized")) {
            return SmtpErrorType.AUTHENTICATION_ERROR;
        }
        
        // Network unreachable
        if (errorMessage.contains("unreachable") || errorMessage.contains("no route")) {
            return SmtpErrorType.NETWORK_UNREACHABLE;
        }
        
        // Port-specific guidance for generic connection failures
        if (port == 25) {
            return SmtpErrorType.PORT_25_BLOCKED;
        } else if (port == 587 && (security == null || security.equals("none"))) {
            return SmtpErrorType.PORT_587_NEEDS_TLS;
        } else if (port == 465 && (security == null || !security.contains("SSL"))) {
            return SmtpErrorType.PORT_465_NEEDS_SSL;
        }
        
        return SmtpErrorType.GENERIC_CONNECTION_FAILURE;
    }
    
    /**
     * Enumeration of SMTP error types with specific guidance.
     */
    private enum SmtpErrorType {
        PORT_25_BLOCKED("Port 25 is commonly blocked by ISPs. Try port 587 with security: 'START_TLS_AUTO'"),
        PORT_587_NEEDS_TLS("Port 587 requires TLS. Add security: 'START_TLS_AUTO' to your configuration"),
        PORT_465_NEEDS_SSL("Port 465 requires SSL. Add security: 'SSL_TLS' to your configuration"),
        CONNECTION_REFUSED("Server rejected the connection. Verify the hostname and port are correct"),
        CONNECTION_TIMEOUT("Connection timed out. Check firewall settings and network connectivity"),
        TLS_SSL_ERROR("TLS/SSL configuration error. Verify security settings match server requirements"),
        AUTHENTICATION_ERROR("Authentication failed. Verify username, password, and account settings"),
        NETWORK_UNREACHABLE("Network unreachable. Check your internet connection and DNS settings"),
        GENERIC_CONNECTION_FAILURE("Connection failed. Verify server hostname, port, and security configuration");
        
        private final String guidance;
        
        SmtpErrorType(String guidance) {
            this.guidance = guidance;
        }
        
        public String getGuidance() {
            return guidance;
        }
    }

}
