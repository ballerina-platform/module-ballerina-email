module io.ballerina.stdlib.email.testutils {
    requires org.slf4j;
    requires greenmail;
    requires io.ballerina.stdlib.mime;
    requires java.mail;
    requires io.ballerina.stdlib.email;
    requires testng;
    exports org.ballerinalang.stdlib.email.testutils;
}
