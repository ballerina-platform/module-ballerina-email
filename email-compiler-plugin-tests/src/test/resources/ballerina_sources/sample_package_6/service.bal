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

    resource function onError a(email:Error emailError) {

    }

}
