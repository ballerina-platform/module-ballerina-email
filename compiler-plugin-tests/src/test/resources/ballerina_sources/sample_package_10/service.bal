import ballerina/email;
import ballerina/io;

listener email:PopListener emailListener = check new ({
    host: "pop.email.com",
    username: "reader@email.com",
    password: "pass456",
    pollingInterval: 2,
    port: 995
});

service "emailObserver" on emailListener {
    remote function onMessage(email:Message emailMessage) {
        io:println("POP Listener received an email.");
    }

    remote function onError(email:Error emailError) {
        io:println("Error while polling for the emails: " + emailError.message());
    }

    remote function onClose(email:Error? closeError) {
        io:println("Closed the listener.");
    }
}