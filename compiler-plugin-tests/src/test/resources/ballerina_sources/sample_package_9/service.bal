import ballerina/email;

service "testService" on new email:UnknownListener({
                                    host: "unknown.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 999
                                }) {

    remote function onMessage(email:Message emailMessage) {

    }

    remote function onError(email:Error emailError) {

    }

    remote function onClose(email:Error? closeError) {

    }

}
