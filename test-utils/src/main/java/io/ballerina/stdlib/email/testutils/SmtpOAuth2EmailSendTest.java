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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.email.testutils;

import io.ballerina.stdlib.email.util.CommonUtil;
import io.ballerina.stdlib.email.util.EmailConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Test class for email send using SMTP with OAuth2 (XOAUTH2 SASL mechanism).
 * Provides a minimal SMTP server that supports XOAUTH2 and a fake OAuth2 token endpoint.
 *
 * @since 2.14.0
 */
public final class SmtpOAuth2EmailSendTest {

    private SmtpOAuth2EmailSendTest() {}

    private static final Logger logger = Logger.getLogger(SmtpOAuth2EmailSendTest.class.getName());

    static final int SMTP_PORT = 3586;
    static final int TOKEN_PORT = 9099;
    static final String ACCESS_TOKEN = "smtp_oauth2_test_token_12345"; // NOSONAR: test-only token
    static final String CLIENT_ID = "test-client-id"; // NOSONAR: test-only credential
    static final String CLIENT_SECRET = "test-client-secret"; // NOSONAR: test-only credential
    private static final String VALID_AUTH_HEADER = // NOSONAR: test-only
            "Basic " + Base64.getEncoder().encodeToString(
                    (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));
    static final String EMAIL_FROM = "someone@localhost.com";
    static final String EMAIL_TO = "hascode@localhost";
    static final String EMAIL_SUBJECT = "OAuth2 Test Email";

    private static final String AUTH_XOAUTH2 = "AUTH XOAUTH2";
    private static final String SMTP_RESPONSE_OK = "250 OK";

    private static ServerSocket smtpServerSocket;
    private static ServerSocket tokenServerSocket;
    private static Thread smtpServerThread;
    private static Thread tokenServerThread;
    private static final List<ReceivedEmail> receivedEmails = new ArrayList<>();

    static class ReceivedEmail {
        String from;
        String to;
        String data;
    }

    public static Object startOAuth2SmtpServer() {
        receivedEmails.clear();
        try {
            smtpServerSocket = new ServerSocket(SMTP_PORT);
            smtpServerThread = new Thread(SmtpOAuth2EmailSendTest::runSmtpServer);
            smtpServerThread.setDaemon(true);
            smtpServerThread.start();

            tokenServerSocket = new ServerSocket(TOKEN_PORT);
            tokenServerThread = new Thread(SmtpOAuth2EmailSendTest::runTokenServer);
            tokenServerThread.setDaemon(true);
            tokenServerThread.start();
        } catch (IOException e) {
            if (smtpServerThread != null) {
                smtpServerThread.interrupt();
            }
            if (smtpServerSocket != null && !smtpServerSocket.isClosed()) {
                try {
                    smtpServerSocket.close();
                } catch (IOException ignored) { }
            }
            return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                    "Failed to start OAuth2 test servers: " + e.getMessage());
        }
        return null;
    }

    private static void runServer(ServerSocket serverSocket, String serverName,
                                   Consumer<Socket> connectionHandler) {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                connectionHandler.accept(client);
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    logger.warning(serverName + " error: " + e.getMessage());
                }
            }
        }
    }

    private static void runSmtpServer() {
        runServer(smtpServerSocket, "SMTP server",
                client -> new Thread(() -> handleSmtpSession(client)).start());
    }

    private static void runTokenServer() {
        runServer(tokenServerSocket, "Token server", SmtpOAuth2EmailSendTest::handleTokenRequest);
    }

    private static void handleSmtpSession(Socket client) {
        try (client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {
            writeLine(out, "220 localhost SMTP test server ready");
            ReceivedEmail email = new ReceivedEmail();
            boolean authenticated = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toUpperCase().startsWith("EHLO") || line.toUpperCase().startsWith("HELO")) {
                    writeLine(out, "250-localhost");
                    writeLine(out, "250-" + AUTH_XOAUTH2);
                    writeLine(out, SMTP_RESPONSE_OK);
                } else if (line.toUpperCase().startsWith(AUTH_XOAUTH2)) {
                    authenticated = handleAuth(line, reader, out);
                } else if (line.toUpperCase().startsWith("MAIL FROM:")) {
                    handleMailFrom(line, authenticated, out, email);
                } else if (line.toUpperCase().startsWith("RCPT TO:")) {
                    email.to = line.substring(8).trim().replaceAll("[<>]", "");
                    writeLine(out, SMTP_RESPONSE_OK);
                } else if (line.equalsIgnoreCase("DATA")) {
                    writeLine(out, "354 Start input; end with <CRLF>.<CRLF>");
                    email.data = readEmailData(reader);
                    synchronized (receivedEmails) {
                        receivedEmails.add(email);
                    }
                    writeLine(out, SMTP_RESPONSE_OK);
                } else if (line.equalsIgnoreCase("QUIT")) {
                    writeLine(out, "221 Bye");
                    break;
                } else if (line.equalsIgnoreCase("RSET")) {
                    email = new ReceivedEmail();
                    authenticated = false;
                    writeLine(out, SMTP_RESPONSE_OK);
                } else if (!line.isEmpty()) {
                    writeLine(out, "500 Unknown command");
                }
            }
        } catch (IOException e) {
            logger.warning("SMTP session error: " + e.getMessage());
        }
    }

    private static boolean handleAuth(String line, BufferedReader reader, OutputStream out) throws IOException {
        String token = extractXoauth2Token(line, reader, out);
        if (ACCESS_TOKEN.equals(token)) {
            writeLine(out, "235 2.7.0 Authentication successful");
            return true;
        }
        writeLine(out, "535 5.7.8 Authentication failed");
        return false;
    }

    private static void handleMailFrom(String line, boolean authenticated, OutputStream out, ReceivedEmail email)
            throws IOException {
        if (!authenticated) {
            writeLine(out, "530 5.7.0 Authentication required");
        } else {
            email.from = line.substring(10).trim().replaceAll("[<>]", "");
            writeLine(out, SMTP_RESPONSE_OK);
        }
    }

    private static String readEmailData(BufferedReader reader) throws IOException {
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !line.equals(".")) {
            data.append(line).append("\n");
        }
        return data.toString();
    }

    private static String extractXoauth2Token(String authLine, BufferedReader reader, OutputStream out)
            throws IOException {
        String encoded;
        if (authLine.length() > AUTH_XOAUTH2.length()) {
            encoded = authLine.substring(AUTH_XOAUTH2.length()).trim();
        } else {
            writeLine(out, "334 ");
            encoded = reader.readLine();
            if (encoded == null) {
                return null;
            }
        }
        if (encoded.isEmpty()) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            // XOAUTH2 format: "user=<email>\x01auth=Bearer <token>\x01\x01"
            for (String part : decoded.split("\u0001")) {
                if (part.startsWith("auth=Bearer ")) {
                    return part.substring("auth=Bearer ".length());
                }
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return null;
    }

    private static void handleTokenRequest(Socket client) {
        try (client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
             OutputStream out = client.getOutputStream()) {
            String authHeader = null;
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("authorization:")) {
                    authHeader = line.substring("authorization:".length()).trim();
                }
            }

            boolean validCredentials = VALID_AUTH_HEADER.equals(authHeader);
            byte[] responseBytes;
            String statusLine;
            if (validCredentials) {
                String response = "{\"access_token\":\"" + ACCESS_TOKEN
                        + "\",\"token_type\":\"Bearer\",\"expires_in\":3600}";
                responseBytes = response.getBytes(StandardCharsets.UTF_8);
                statusLine = "200 OK";
            } else {
                String error = "{\"error\":\"invalid_client\"}";
                responseBytes = error.getBytes(StandardCharsets.UTF_8);
                statusLine = "401 Unauthorized";
            }
            String httpResponse = "HTTP/1.1 " + statusLine + "\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + responseBytes.length + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";
            out.write(httpResponse.getBytes(StandardCharsets.UTF_8));
            out.write(responseBytes);
            out.flush();
        } catch (IOException e) {
            logger.warning("Token request error: " + e.getMessage());
        }
    }

    private static void writeLine(OutputStream out, String line) throws IOException {
        out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public static Object stopOAuth2SmtpServer() {
        for (ServerSocket socket : new ServerSocket[]{smtpServerSocket, tokenServerSocket}) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.warning("Error stopping server: " + e.getMessage());
                }
            }
        }
        return null;
    }

    public static Object validateOAuth2Email() {
        synchronized (receivedEmails) {
            if (receivedEmails.isEmpty()) {
                return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                        "No messages received by the SMTP server via OAuth2");
            }
            ReceivedEmail email = receivedEmails.get(0);
            if (!EMAIL_FROM.equals(email.from)) {
                return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                        "Expected from=" + EMAIL_FROM + " but got: " + email.from);
            }
            if (!EMAIL_TO.equals(email.to)) {
                return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                        "Expected to=" + EMAIL_TO + " but got: " + email.to);
            }
            if (!email.data.contains(EMAIL_SUBJECT)) {
                return CommonUtil.getBallerinaError(EmailConstants.ERROR,
                        "Email data does not contain expected subject: " + EMAIL_SUBJECT);
            }
        }
        return null;
    }
}
