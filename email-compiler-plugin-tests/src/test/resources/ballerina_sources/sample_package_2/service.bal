import ballerina/email;

service "testService" on new email:PopListener({
                                    host: "pop.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 995
                                }) {

    function onMessage(email:Message emailMessage) {

    }

    function onError(email:Error emailError) {

    }

    function onClose(email:Error? closeError) {

    }

}
