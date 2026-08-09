/*
 * Copyright 2013-present the original author or authors.
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
package org.eulerframework.boot.autoconfigure.support.security.login.password;

import org.eulerframework.boot.autoconfigure.support.security.login.BaseLoginMethodConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "euler.security.login-method")
public class EulerSecurityLoginMethodPasswordProperties {
    private final Map<String, LoginMethodPasswordConfiguration> password = new LinkedHashMap<>();

    public Map<String, LoginMethodPasswordConfiguration> getPassword() {
        return password;
    }

    /**
     * A {@code password} method declaration. Credentials are
     * verified against the local user store, so it establishes no
     * identity and takes no settings of its own.
     */
    public static class LoginMethodPasswordConfiguration extends BaseLoginMethodConfiguration {
    }
}
