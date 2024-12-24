/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.socket;

import org.eulerframework.socket.configurers.EulerSocketServerConfiguration;
import org.eulerframework.socket.dispatcher.MessageDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

public class EulerSocketServerInitializingListener implements ApplicationListener<ApplicationPreparedEvent> {
    private final Logger logger = LoggerFactory.getLogger(EulerSocketServerInitializingListener.class);

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        MessageDispatcher<?> messageDispatcher = applicationContext.getBean(MessageDispatcher.class);
        EulerSocketServerConfiguration.setupMessageDispatcher(messageDispatcher, applicationContext);
    }
}
