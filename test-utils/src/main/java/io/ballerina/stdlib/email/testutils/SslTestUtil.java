/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package io.ballerina.stdlib.email.testutils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility for configuring GreenMail's SSL to use a fresh test keystore.
 * GreenMail reads the system property {@code greenmail.tls.keystore.file} at startup
 * (before the server socket factory is initialized) to override its bundled keystore.
 *
 * @since 2.14.0
 */
final class SslTestUtil {

    private static final String KEYSTORE_RESOURCE = "greenmail_test.p12";
    private static final String KEYSTORE_PASSWORD = "changeit"; // NOSONAR: test-only keystore password

    private SslTestUtil() {}

    private static volatile boolean configured = false;

    /**
     * Extracts the bundled test keystore to a temp file and points GreenMail at it.
     * Safe to call multiple times — only acts on the first call.
     */
    static synchronized void configureSsl() throws IOException {
        if (configured) {
            return;
        }
        // Try Module API first (works for named JPMS modules), fall back to ClassLoader for unnamed modules
        InputStream is = SslTestUtil.class.getModule().getResourceAsStream(KEYSTORE_RESOURCE);
        if (is == null) {
            is = SslTestUtil.class.getClassLoader().getResourceAsStream(KEYSTORE_RESOURCE);
        }
        if (is == null) {
            throw new IOException("Test keystore resource not found: " + KEYSTORE_RESOURCE);
        }
        final InputStream resolvedIs = is;
        Path tmpKeystore = Files.createTempFile("greenmail_test", ".p12");
        tmpKeystore.toFile().deleteOnExit();
        try (resolvedIs; OutputStream fos = Files.newOutputStream(tmpKeystore)) {
            resolvedIs.transferTo(fos);
        }
        System.setProperty("greenmail.tls.keystore.file", tmpKeystore.toAbsolutePath().toString());
        System.setProperty("greenmail.tls.keystore.password", KEYSTORE_PASSWORD);
        System.setProperty("greenmail.tls.key.password", KEYSTORE_PASSWORD);
        configured = true;
    }
}
