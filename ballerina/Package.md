## Package Overview

This package provides APIs to perform email operations such as sending and reading emails using the SMTP, POP3, and IMAP4 protocols.

### Client

This package supports the following three client types.

**email:SmtpClient**: The client, which supports sending an email using the SMTP protocol.

**email:PopClient**: The client, which supports receiving an email using the POP3 protocol.

**email:ImapClient**: The client, which supports receiving an email using the IMAP4 protocol.

#### SMTP client

To send an email using the SMTP protocol, you must first create an `email:SmtpClient` object. The code for creating an `email:SmtpClient` can be found
 below.

##### Create a client

The following code creates an SMTP client, which connects to the default port (i.e. 465) and enables SSL.
```ballerina
email:SmtpClient smtpClient = check new ("smtp.email.com", "sender@email.com", "pass123");
```
The port number of the server can be configured by passing the following configurations.

```ballerina
email:SmtpConfiguration smtpConfig = {
    port: 465
};

email:SmtpClient smtpClient = check new ("smtp.email.com", "sender@email.com", "pass123", smtpConfig);
```

##### Send an email

Once the `email:SmtpClient` is created, an email can be sent using the SMTP protocol through that client.
Samples for this operation can be found below.

```ballerina
email:Message email = {
    to: ["receiver1@email.com", "receiver2@email.com"],
    cc: ["receiver3@email.com", "receiver4@email.com"],
    bcc: ["receiver5@email.com"],
    subject: "Sample Email",
    body: "This is a sample email.",
    'from: "author@email.com",
    sender: "sender@email.com",
    replyTo: ["replyTo1@email.com", "replyTo2@email.com"]
};

check smtpClient->sendMessage(email);
```

An email can be sent directly by calling the client specifying optional parameters as named parameters as well.
Samples for this operation can be found below.

```ballerina
email:Error? response = smtpClient->send(
    ["receiver1@email.com", "receiver2@email.com"],
    "Sample Email",
    "author@email.com",
    body="This is a sample email.",
    cc=["receiver3@email.com", "receiver4@email.com"],
    bcc=["receiver5@email.com"],
    sender="sender@email.com",
    replyTo=["replyTo1@email.com", "replyTo2@email.com"]
);
```

#### POP3 client

To receive an email using the POP3 protocol, you must first create an `email:PopClient` object. The code for creating an
 `email:PopClient` can be found below.

##### Create a client

The following code creates a POP3 client, which connects to the default port (i.e. 995) and enables SSL.
```ballerina
email:PopClient popClient = check new ("pop.email.com", "reader@email.com", "pass456");
```

The port number of the server can be configured by passing the following configurations.
```ballerina
email:PopConfiguration popConfig = {
    port: 995
};

email:PopClient popClient = check new ("pop.email.com", "reader@email.com", "pass456", popConfig);
```

##### Receive an email
Once the `email:PopClient` is created, emails can be received using the POP3 protocol through that client.
Samples for this operation can be found below.

```ballerina
email:Message? emailResponse = check popClient->receiveMessage();
```

#### IMAP4 client

To receive an email using the IMAP4 protocol, you must first create an `email:ImapClient` object. The code for creating an
 `email:ImapClient` can be found below.

##### Create a client

The following code creates an IMAP4 client, which connects to the default port (i.e. 993) and enables SSL.
```ballerina
email:ImapClient imapClient = check new ("imap.email.com", "reader@email.com", "pass456");
```

The port number of the server can be configured by passing the following configuration.
```ballerina
email:ImapConfiguration imapConfig = {
    port: 993
};

email:ImapClient imapClient = check new ("imap.email.com", "reader@email.com", "pass456", imapConfig);
```

##### Receive an email
Once the `email:ImapClient` is created, emails can be received using the IMAP4 protocol through that client.
Samples for this operation can be found below.

```ballerina
email:Message? emailResponse = check imapClient->receiveMessage();
```

#### POP3 and IMAP listeners

As POP3 and IMAP4 protocols are similar in the listener use cases, POP3 is considered in the examples below.
In order to receive emails one-by-one from a POP3 server, you must first create an `email:PopListener` object.
The code for creating an `email:PopListener` can be found below.

```ballerina
listener email:PopListener emailListener = check new ({
    host: "pop.email.com",
    username: "reader@email.com",
    password: "pass456",
    pollingInterval: 2,
    port: 995
});
```

Once initialized, a `service` can listen to the new emails as follows. New emails get received at the `onMessage`
method and when errors happen, the `onError` method gets called.

```ballerina
service "emailObserver" on emailListener {

    remote function onMessage(email:Message emailMessage) {
        io:println("Email Subject: ", emailMessage.subject);
        io:println("Email Body: ", emailMessage.body);
    }

    remote function onError(email:Error emailError) {
        io:println("Error while polling for the emails: " + emailError.message());
    }

}
```

### Security and authentication

The `email` package supports both the TLS/SSL and STARTTLS as transport-level security protocols.

Transport-level security for all SMTP, POP3, and IMAP clients/listeners can be configured with the `secureSocket` field.

```ballerina
secureSocket: {
    cert: "path/to/certfile.crt",
    protocol: {
        name: TLS,
        versions: ["TLSv1.2", "TLSv1.1"]
    },
    ciphers: ["TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA"],
    verifyHostName: true
}
```

Transport-level security for the SMTP client configuration can be defined as follows.

```ballerina
email:SmtpConfiguration smtpConfig = {
    port: 465,
    secureSocket: {
        // Transport level configuration
    }
};
```

Transport-level security for the POP3 client configuration can be defined as follows.

```ballerina
email:PopConfiguration popConfig = {
     port: 995,
     secureSocket: {
         // Transport level configuration
     }
};
```

Transport-level security for the IMAP client configuration can be defined as follows.

```ballerina
email:ImapConfiguration imapConfig = {
     port: 993,
     secureSocket: {
         // Transport level configuration
     }
};
```

Transport-level security for the POP3 listener configuration can be defined as follows.

```ballerina
email:PopListenerConfiguration popListenerConfig = {
    host: "127.0.0.1",
    username: "hascode",
    password: "abcdef123",
    pollingInterval: 2,
    port: 995,
    secureSocket: {
        // Transport level configuration
    }
};
```

Transport-level security for the IMAP listener configuration can be defined as follows.

```ballerina
email:ImapListenerConfiguration imapListenerConfig = {
    host: "127.0.0.1",
    username: "hascode",
    password: "abcdef123",
    pollingInterval: 2,
    port: 993,
    secureSocket: {
        // Transport level configuration
    }
};
```

By default, TLS/SSL is enabled as the default transport-level security protocol, and the certificate verification is set as required.
This optional protocol definition can be configured with the `security` `enum` field in each of the configuration types described above.

The options available with the `security` field are as follows.

**SSL**: As same as the default TLS/SSL protocol

**START_TLS_NEVER**: Disables both TLS/SSL and STARTTLS protocols and allows only the unencrypted transport-level communication

**START_TLS_ALWAYS**: Makes it mandatory to use the secure STARTTLS protocol

**START_TLS_AUTO**: Enables the STARTTLS protocol, which would switch to the unsecured communication mode if the secure STARTTLS mode is not available in the server

The following is an example of using the `security` field in the SMTP client with the `START_TLS_AUTO` mode.

```ballerina
email:SmtpConfiguration smtpConfig = {
    port: 587,
    secureSocket: {
        // Transport level configuration
    },
    security: START_TLS_AUTO
};
```

Similarly, other client/listener configuration types can also be defined with the `security` field.

**Note**: Make sure the port number is changed accordingly depending on the protocol used.

Standard port numbers used for each of the protocol for each type of transport security are as given below.

| Protocol/Security | SSL | STARTTLS | Unsecure |
|-------------------|-----|----------|----------|
| **SMTP**          | 465 | 587      | 25, 587  |
| **POP3**          | 995 | 995      | 110      |
| **IMAP4**         | 993 | 143, 993 | 143      |

All the authentications are based on the username/password credentials.

>**Note:** When the `'from` field is not provided in an `email:Message`, the `username` field of the initialization argument of the `email:SmtpClient` is set as the `from` address of an email to be sent with SMTP.

### Message content and 

An `email:Message` prepared to be sent can have the text body content, `body`, and/or HTML body content (`htmlBody`).
When emails are received with POP3 or IMAP, the text email bodies and HTML bodies of the email are captured by the `body` and `htmlBody` fields of the `email:Message` respectively.

When sending emails with SMTP, there are four options to specify the email `attachments` in the `email:Message`.

1. With the `email:Attachment` type, which points to an attachment file along with its content-type
2. With an array of the `email:Attachment` type
3. With the `mime:Entity` type
4. With an array of the `mime:Entity` type

Option 1 and 2 are designed for ordinary users to attach files from the local machine along with its content-type.
Option 3 and 4 are designed for advanced users who have programming knowledge to define complex MIME typed data attachments.

The following is an example of attaching a PDF file to an email with option 1.

```ballerina
email:Attachment pdfAttachment = {filePath: "path/to/application.pdf", contentType: "application/pdf"};

email:Message email = {
    // Other fields
    attachments: pdfAttachment
};
```

The following is an example of attaching a JPG file to an email with option 3.

```ballerina
mime:Entity imageAttachment = new;
mime:ContentDisposition disposition = new;
disposition.fileName = "profilePic.jpg";
disposition.disposition = "attachment";
disposition.name = "profilePic";
imageAttachment.setContentDisposition(disposition);
imageAttachment.setContentId("ImageAttachment");
imageAttachment.setFileAsEntityBody("path/to/profilePic.jpg", mime:IMAGE_JPEG);

email:Message email = {
    // Other fields
    attachments: imageAttachment
};
```

## Report issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the <a target="_blank" href="https://github.com/ballerina-platform/ballerina-standard-library">Ballerina standard library parent repository</a>.

## Useful links

- Chat live with us via our <a target="_blank" href="https://ballerina.io/community/slack/">Slack channel</a>.
- Post all technical questions on Stack Overflow with the <a target="_blank" href="https://stackoverflow.com/questions/tagged/ballerina">#ballerina</a> tag.
