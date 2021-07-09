import ballerina/email;

service "testPopService" on new email:PopListener({
                                    host: "pop.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 995
                                }) {

    remote function onMessage(email:Message emailMessage) {

    }

    remote function onError(email:Error emailError) {

    }

    remote function onClose(email:Error? closeError) {

    }

}

service "testImapService" on new email:ImapListener({
                                    host: "imap.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 993
                                }) {

    remote function onMessage(email:Message emailMessage) {

    }

    remote function onError(email:Error emailError) {

    }

    remote function onClose(email:Error? closeError) {

    }

    function prepareEmail() {

    }

}
