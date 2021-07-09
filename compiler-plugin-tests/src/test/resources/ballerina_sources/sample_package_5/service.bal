import ballerina/email;

service "testService" on new email:PopListener({
                                    host: "pop.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 995
                                }) {

    remote function onMessage(string emailMessage) {

    }

    remote function onError(error emailError) {

    }

    remote function onClose(string closeError) {

    }

}
