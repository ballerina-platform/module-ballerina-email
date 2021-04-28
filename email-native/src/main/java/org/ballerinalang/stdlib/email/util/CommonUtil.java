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

import com.sun.mail.util.MailSSLSocketFactory;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import org.ballerinalang.mime.util.MimeConstants;
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
        return ErrorCreator.createDistinctError(typeId, EmailUtils.getEmailPackage(),
                StringUtils.fromString(message));
    }

}
