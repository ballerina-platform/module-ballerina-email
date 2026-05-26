# SMTP Client for Gmail

## Overview

This guide explains how to securely send an email using the SMTP API of Gmail using Ballerina. 
The figure below illustrates a high-level design diagram of the complete use case.

<div align="center"><img src="smtp-client-for-gmail.jpg" alt="Send an email using Gmail SMTP API" width="500"/></div>

The below are the detailed explanations of each of the steps.

### Step 1 - Set Up Gmail SMTP Access

> **Note:** Google removed the "Less Secure Apps" setting in May 2022. Username and password authentication no longer works for
> personal Gmail accounts. To send emails via Gmail SMTP, use one of the following approaches:
>
> - **Google Workspace / personal accounts with App Passwords**: Generate an App Password in your Google Account security
>   settings (requires 2-Step Verification to be enabled) and use it as the password.
> - **OAuth2 (recommended)**: Use the XOAUTH2 SASL mechanism with a valid OAuth2 access token. See the
>   [`smtp-oauth2-client`](../smtp-oauth2-client) example for a complete walkthrough.

This example uses an App Password. Generate one at <https://myaccount.google.com/apppasswords> and use it in place of
`senderPassword` in the configuration below.

### Step 2 - Initialize the Email Client with Credentials

In order to send an email, an SMTP Client has to be initialized with the Gmail server related connection details and user's
account credentials. By default, the Ballerina SMTP client is configured to run on port `465` with SSL, which is also used
in Gmail. As Gmail server certificates are signed by certificate authorities, there is no need to configure them in the
client.

```ballerina
    email:SmtpClient smtpClient = check new ("smtp.gmail.com", senderAddress, senderPassword);
```

### Step 3 - Define the Email Message Content

An `email:Message` record should be defined with the content to be sent along with the email. When the email is sent using
a Gmail server, the `'from` field can be excluded as it is the same as the username specified during the client initialization.

### Step 4 - Send the Defined Email Using the Client

Any number of different `email:Message` records can be used to send as emails sequentially using the initialized SMTP client.

## Testing

You can run the above code in your local environment. Navigate to the
[`examples/gmail-smtp-client/client`](./client) directory, and execute the command below.
```shell
$ bal run
```

The successful execution of the service should show the output below.
```shell
Compiling source
	smtp/gmail_sender:1.0.0

Running executable
```

Now, check the inbox of the email receiver. The sent email should be available after some time.
