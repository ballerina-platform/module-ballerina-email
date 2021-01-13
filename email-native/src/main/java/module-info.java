/*
 * Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

module io.ballerina.stdlib.email {
    exports org.ballerinalang.stdlib.email.util;
    exports org.ballerinalang.stdlib.email.client;
    exports org.ballerinalang.stdlib.email.service.compiler;
    requires io.ballerina.runtime;
    requires io.ballerina.tools.api;
    requires org.slf4j;
    requires io.ballerina.lang;
    requires java.mail;
    requires io.ballerina.stdlib.mime;
    requires io.ballerina.stdlib.io;
}
