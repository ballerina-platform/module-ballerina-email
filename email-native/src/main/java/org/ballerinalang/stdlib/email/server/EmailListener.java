/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.email.server;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import org.ballerinalang.stdlib.email.util.EmailConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_CLOSE_METADATA;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_ERROR_METADATA;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_MESSAGE;
import static org.ballerinalang.stdlib.email.util.EmailConstants.ON_MESSAGE_METADATA;

/**
 * Email connector listener for Ballerina.
 *
 * @since 1.3.0
 */
public class EmailListener {

    private static final Logger log = LoggerFactory.getLogger(EmailListener.class);

    private final Runtime runtime;

    private Map<String, BObject> registeredServices = new HashMap<>();

    /**
     * Constructor for listener class for email.
     * @param runtime Current Ballerina runtime
     */
    public EmailListener(Runtime runtime) {
        this.runtime = runtime;
    }

    /**
     * Place an email in Ballerina when received.
     * @param emailEvent Email object to be received
     * @return If successful return true
     */
    public boolean onMessage(EmailEvent emailEvent) {
        Object email = emailEvent.getEmailObject();
        if (runtime != null) {
            Set<Map.Entry<String, BObject>> services = registeredServices.entrySet();
            for (Map.Entry<String, BObject> service : services) {
                runtime.invokeMethodAsync(service.getValue(), ON_MESSAGE, null, ON_MESSAGE_METADATA,
                                          new Callback() {
                    @Override
                    public void notifySuccess(Object o) {
                    }

                    @Override
                    public void notifyFailure(BError error) {
                        log.error("Error while invoking email onMessage method.");
                    }
                }, email, true);
            }
        } else {
            log.error("Runtime should not be null.");
        }
        return true;
    }

    /**
     * Place an error in Ballerina when received.
     * @param error Email object to be received
     */
    public void onError(Object error) {
        log.error(((BError) error).getMessage());
        if (runtime != null) {
            Set<Map.Entry<String, BObject>> services = registeredServices.entrySet();
            for (Map.Entry<String, BObject> service : services) {
                runtime.invokeMethodAsync(service.getValue(), EmailConstants.ON_ERROR, null, ON_ERROR_METADATA,
                        new Callback() {
                    @Override
                    public void notifySuccess(Object o) {
                    }

                    @Override
                    public void notifyFailure(BError error) {
                        log.error("Error while invoking email onMessage method.");
                    }
                }, error, true);
            }
        } else {
            log.error("Runtime should not be null.");
        }
    }

    /**
     * Place an error in Ballerina if error has occurred while closing.
     * @param error Email object to be received
     */
    public void onClose(Object error) {
        if (error != null) {
            log.error(((BError) error).getMessage());
        }
        if (runtime != null) {
            Set<Map.Entry<String, BObject>> services = registeredServices.entrySet();
            for (Map.Entry<String, BObject> service : services) {
                runtime.invokeMethodAsync(service.getValue(), EmailConstants.ON_CLOSE, null, ON_CLOSE_METADATA,
                        new Callback() {
                            @Override
                            public void notifySuccess(Object o) {
                            }

                            @Override
                            public void notifyFailure(BError error) {
                                log.error("Error while closing the POP3/IMAP connection.");
                            }
                        }, error, true);
            }
        } else {
            log.error("Runtime should not be null.");
        }
    }

    protected void addService(BObject service) {
        if (service != null && service.getType() != null && service.getType().getName() != null) {
            registeredServices.put(service.getType().getName(), service);
        }
    }

}
