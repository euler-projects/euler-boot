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
package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityProperties;
import org.eulerframework.security.web.endpoint.user.login.RegisteredLoginMethod;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Maps {@code euler.security.login-method} declarations from
 * {@link EulerBootSecurityProperties} to immutable
 * {@link RegisteredLoginMethod}s, following the pattern of Spring
 * Boot's {@code OAuth2ClientPropertiesMapper}.
 */
public final class LoginMethodPropertiesMapper {

    private final Map<String, EulerBootSecurityProperties.LoginMethod> loginMethods;

    public LoginMethodPropertiesMapper(EulerBootSecurityProperties properties) {
        Assert.notNull(properties, "properties must not be null");
        this.loginMethods = properties.getLoginMethod();
    }

    /**
     * Returns the declared login methods as immutable registrations,
     * each carrying its declaration key as {@code id}. Returns an empty
     * list when nothing is declared.
     *
     * @throws IllegalStateException if a declared method has no
     *                               {@code method-type}
     */
    public List<RegisteredLoginMethod> asRegisteredLoginMethods() {
        List<RegisteredLoginMethod> registered = new ArrayList<>(this.loginMethods.size());
        this.loginMethods.forEach((key, method) -> {
            if (method == null) {
                return;
            }
            if (method.getMethodType() == null || method.getMethodType().isEmpty()) {
                throw new IllegalStateException("Login method '" + key
                        + "' declares no 'method-type' under euler.security.login-method.");
            }
            registered.add(asRegisteredLoginMethod(key, method));
        });
        return registered;
    }

    private static RegisteredLoginMethod asRegisteredLoginMethod(
            String key, EulerBootSecurityProperties.LoginMethod method) {
        return RegisteredLoginMethod.withId(key)
                .type(method.getMethodType())
                .name(method.getMethodName())
                .identityType(method.getIdentityType())
                .primary(method.isPrimary())
                .properties(method.getProperties())
                .build();
    }
}
