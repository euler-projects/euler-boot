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

package org.eulerframework.boot.autoconfigure.support.socket;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "euler.socket.server")
public class EulerProtoSocketServerProperties {
    private int port = 18080;
    private boolean enableSession = true;
    private boolean enableHaproxy = false;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnableSession() {
        return enableSession;
    }

    public void setEnableSession(boolean enableSession) {
        this.enableSession = enableSession;
    }

    public boolean isEnableHaproxy() {
        return enableHaproxy;
    }

    public void setEnableHaproxy(boolean enableHaproxy) {
        this.enableHaproxy = enableHaproxy;
    }
}
