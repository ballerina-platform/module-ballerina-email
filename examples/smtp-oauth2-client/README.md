# SMTP Client with OAuth2

## Overview

This example shows how to send an email over SMTP using OAuth2 (XOAUTH2 SASL mechanism) with a Client Credentials Grant. The `ballerina/email` module fetches the access token at client initialization and refreshes it automatically when it expires.

### Prerequisites

- An SMTP server that supports the XOAUTH2 SASL mechanism (e.g., Gmail with OAuth2, Microsoft Exchange Online).
- An OAuth2 authorization server issuing tokens for your SMTP account (a Client Credentials or Password Grant endpoint).

### Configuration

Edit `client/Config.toml` with your values:

```toml
[smtp_oauth2_sender]
senderAddress    = "sender@example.com"
receiverAddress  = "receiver@example.com"
tokenUrl         = "https://oauth2.provider.com/token"
clientId         = "your-client-id"
clientSecret     = "your-client-secret"
```

Also update the `smtp.example.com` hostname and port in `smtp_oauth2_sender.bal` to match your SMTP server.

### Run

Navigate to the `client/` directory and execute:

```shell
$ bal run
```

A successful run produces no output. Check the recipient's inbox for the delivered message.

## Gmail-specific setup

1. Create a Google Cloud project and enable the Gmail API.
2. Create OAuth2 credentials (client ID and secret) with the scope `https://mail.google.com/`.
3. Use Google's token endpoint `https://oauth2.googleapis.com/token` and a service-account or user-delegated flow.

For a simpler App Password approach, see the [`gmail-smtp-client`](../gmail-smtp-client) example.
