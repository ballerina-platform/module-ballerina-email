/*
 * Copyright (c) 2024 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.mail.MessagingException;

/**
 * Unit tests for enhanced SMTP error messages.
 * Tests various failure scenarios to ensure proper error categorization and guidance.
 */
public class SmtpErrorEnhancementTest {

    @Test
    public void testPort25ConnectionRefused() {
        MessagingException exception = new MessagingException("Connection refused");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.gmail.com", 25, null);
        
        Assert.assertTrue(result.contains("smtp.gmail.com:25"));
        Assert.assertTrue(result.contains("Port 25 is commonly blocked"));
        Assert.assertTrue(result.contains("Try port 587"));
        Assert.assertTrue(result.contains("START_TLS_AUTO"));
    }

    @Test
    public void testPort587WithoutTLS() {
        MessagingException exception = new MessagingException("Could not connect to SMTP host: smtp.outlook.com, port: 587");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.outlook.com", 587, null);
        
        Assert.assertTrue(result.contains("smtp.outlook.com:587"));
        Assert.assertTrue(result.contains("Port 587 requires TLS"));
        Assert.assertTrue(result.contains("START_TLS_AUTO"));
    }

    @Test
    public void testPort465WithoutSSL() {
        MessagingException exception = new MessagingException("SSL connection failed");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.yahoo.com", 465, "none");
        
        Assert.assertTrue(result.contains("smtp.yahoo.com:465"));
        Assert.assertTrue(result.contains("Port 465 requires SSL"));
        Assert.assertTrue(result.contains("SSL_TLS"));
    }

    @Test
    public void testConnectionTimeout() {
        MessagingException exception = new MessagingException("Connection timed out");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.example.com", 587, "START_TLS_AUTO");
        
        Assert.assertTrue(result.contains("Connection timed out"));
        Assert.assertTrue(result.contains("Check firewall settings"));
        Assert.assertTrue(result.contains("network connectivity"));
    }

    @Test
    public void testAuthenticationError() {
        MessagingException exception = new MessagingException("Authentication failed");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.gmail.com", 587, "START_TLS_AUTO");
        
        Assert.assertTrue(result.contains("Authentication failed"));
        Assert.assertTrue(result.contains("Verify username, password"));
    }

    @Test
    public void testTLSError() {
        MessagingException exception = new MessagingException("STARTTLS is required but not supported");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.example.com", 587, null);
        
        Assert.assertTrue(result.contains("Port 587 requires TLS"));
        Assert.assertTrue(result.contains("START_TLS_AUTO"));
    }

    @Test
    public void testGenericConnectionFailure() {
        MessagingException exception = new MessagingException("Unknown connection error");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.custom.com", 2525, "START_TLS_AUTO");
        
        Assert.assertTrue(result.contains("smtp.custom.com:2525"));
        Assert.assertTrue(result.contains("Connection failed"));
        Assert.assertTrue(result.contains("Verify server hostname"));
    }

    @Test
    public void testErrorMessageFormat() {
        MessagingException exception = new MessagingException("Connection refused");
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.test.com", 25, null);
        
        // Verify the error message follows the expected format
        Assert.assertTrue(result.startsWith("SMTP connection to smtp.test.com:25 failed:"));
        Assert.assertTrue(result.contains("Connection refused"));
        
        // Ensure the message is concise (not overly verbose)
        Assert.assertTrue(result.length() < 200, "Error message should be concise");
    }

    @Test
    public void testNullExceptionMessage() {
        MessagingException exception = new MessagingException((String) null);
        String result = CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.test.com", 587, null);
        
        Assert.assertTrue(result.contains("smtp.test.com:587"));
        Assert.assertTrue(result.contains("Port 587 requires TLS"));
    }

    @Test
    public void testPerformance() {
        MessagingException exception = new MessagingException("Connection refused");
        
        // Measure time for 1000 error message generations
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            CommonUtil.createEnhancedSmtpErrorMessage(exception, "smtp.test.com", 25, null);
        }
        long endTime = System.nanoTime();
        
        long durationMs = (endTime - startTime) / 1_000_000;
        Assert.assertTrue(durationMs < 100, "Error message generation should be fast: " + durationMs + "ms");
    }
}