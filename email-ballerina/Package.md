## Package Overview

This package contains functions to perform email operations such as sending and reading emails using the SMTP, POP3, and IMAP4 protocols.

### Client

This package supports the following three client types.

- `email:SmtpClient` - The client, which supports sending an email using the SMTP protocol.
- `email:PopClient` - The client, which supports receiving an email using the POP3 protocol.
- `email:ImapClient` - The client, which supports receiving an email using the IMAP4 protocol.

#### SMTP Client

To send an email using the SMTP protocol, you must first create an `email:SmtpClient` object. The code for creating an `email:SmtpClient` can be found
 below.

##### Creating a client

The following code creates an SMTP client, which connects to the default port(465) and enables SSL.
```ballerina
email:SmtpClient smtpClient = new ("smtp.email.com",
                                   "sender@email.com",
                                   "pass123");
```
The port number of the server can be configured by passing the following configurations.

```ballerina
email:SmtpConfig smtpConfig = {
    port: 465 // Can use ports, 465, 587 or 25
};

email:SmtpClient smtpClient = new ("smtp.email.com",
                                   "sender@email.com",
                                   "pass123",
                                    smtpConfig);
```

##### Sending an email

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

email:Error? response = smtpClient->sendMessage(email);
```

An email can be sent directly by calling the client, specifying optional parameters as named parameters, as well.
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

#### POP3 Client

To receive an email using the POP3 protocol, you must first create an `email:PopClient` object. The code for creating an
 `email:PopClient` can be found below.

##### Creating a client

The following code creates a POP3 client, which connects to the default port(995) and enables SSL.
```ballerina
email:PopClient|email:Error popClient = new ("pop.email.com",
                                             "reader@email.com",
                                             "pass456");
```

The port number of the server can be configured by passing the following configurations.
```ballerina
email:PopConfig popConfig = {
    port: 995
};

email:PopClient|email:Error popClient = new ("pop.email.com",
                                             "reader@email.com",
                                             "pass456",
                                              popConfig);
```

##### Receiving an email
Once the `email:PopClient` is created, emails can be received using the POP3 protocol through that client.
Samples for this operation can be found below.

```ballerina
email:Message|email:Error? emailResponse = popClient->receiveEmailMessage();
```

#### IMAP4 Client

To receive an email using the IMAP4 protocol, you must first create an `email:ImapClient` object. The code for creating an
 `email:ImapClient` can be found below.

##### Creating a client

The following code creates an IMAP4 client, which connects to the default port(993) and enables SSL.
```ballerina
email:ImapClient|email:Error imapClient = new ("imap.email.com",
                                               "reader@email.com",
                                               "pass456");
```

The port number of the server and/or the SSL support can also be configured by passing the following configurations.
```ballerina
email:ImapConfig imapConfig = {
    port: 993,
    enableSsl: true
};

email:ImapClient|email:Error imapClient = new ("imap.email.com",
                                               "reader@email.com",
                                               "pass456",
                                                imapConfig);
```

##### Receiving an email
Once the `email:ImapClient` is created, emails can be received using the IMAP4 protocol through that client.
Samples for this operation can be found below.

```ballerina
email:Message|email:Error emailResponse = imapClient->receiveEmailMessage();
```

#### POP3 and IMAP Listeners

As POP3 and IMAP4 protocols are similar in the listener use cases, lets consider POP3.
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

Once initialized a `service` can listen to the new emails as follows. New emails get received at the `onEmailMessage`
method and when errors happen `onError` method get called.

```ballerina
service "emailObserver" on emailListener {

    remote function onEmailMessage(email:Message emailMessage) {
        io:println("Email Subject: ", emailMessage.subject);
        io:println("Email Body: ", emailMessage.body);
    }

    remote function onError(email:Error emailError) {
        io:println("Error while polling for the emails: " + emailError.message());
    }

}
```

For information on the operations, which you can perform with this package, see the **Functions**  below. For examples of the usage of the operation, see the following.
  * [Send Emails Example](https://ballerina.io/learn/by-example/send-email.html)
  * [Receive Emails using a client Example](https://ballerina.io/learn/by-example/receive-email-using-client.html)
  * [Receive Emails using a listener Example](https://ballerina.io/learn/by-example/receive-email-using-listener.html)
