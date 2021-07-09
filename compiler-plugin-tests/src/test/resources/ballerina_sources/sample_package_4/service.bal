import ballerina/email;

service "testService" on new email:PopListener({
                                    host: "pop.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 995
                                }) {

    remote function onMessage(email:Message emailMessage) {

    }

    remote function onMessageReceived(email:Error? changeError) {

    }

    remote function onError(email:Error emailError) {

    }

    remote function onClose(email:Error? closeError) {

    }

    remote function onChanged(email:Error? changeError) {

    }

}
