module io.ballerina.stdlib.email {
    exports org.ballerinalang.stdlib.email.util;
    exports org.ballerinalang.stdlib.email.client;
    exports org.ballerinalang.stdlib.email.service.compiler;
    requires io.ballerina.jvm;
    requires org.slf4j;
    requires io.ballerina.lang;
    requires java.mail;
    requires io.ballerina.stdlib.mime;
    requires io.ballerina.stdlib.io;
}