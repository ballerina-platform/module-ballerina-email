import ballerina/email;

service "testService" on new email:PopListener({
                                    host: "pop.example.com",
                                    username: "abc@example.com",
                                    password: "pass123",
                                    pollingInterval: 2,
                                    port: 995
                                }) {

    remote function onMessage(email:Message emailMessage) returns email:Message {
        return {to: "abc@example.com", subject: "Sample Subject", body: "A sample text body"};
    }

    remote function onError(email:Error emailError) returns email:Error {
        return <email:Error>error("Something is wrong");
    }

    remote function onClose(email:Error? closeError) returns email:Error? {

    }

}
