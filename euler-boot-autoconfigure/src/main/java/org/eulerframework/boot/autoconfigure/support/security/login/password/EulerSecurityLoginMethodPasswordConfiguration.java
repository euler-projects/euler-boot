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
import org.eulerframework.boot.autoconfigure.support.security.login.LoginMethodConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebEndpointProperties;
import org.eulerframework.security.web.login.PasswordLoginMethodHandler;
import org.eulerframework.security.web.login.RegisteredLoginMethod;
import org.eulerframework.security.web.login.RegisteredPasswordLoginMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({EulerSecurityLoginMethodPasswordProperties.class})
public class EulerSecurityLoginMethodPasswordConfiguration implements LoginMethodConfiguration {

    private final EulerSecurityLoginMethodPasswordProperties properties;

    public EulerSecurityLoginMethodPasswordConfiguration(EulerSecurityLoginMethodPasswordProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(PasswordLoginMethodHandler.class)
    public PasswordLoginMethodHandler passwordLoginMethodHandler(
            EulerBootSecurityWebEndpointProperties endpointProperties) {
        return new PasswordLoginMethodHandler(
                endpointProperties.getUser().getLoginProcessingUrl(),
                endpointProperties.getUser().getLoginPage(),
                endpointProperties.getLoginMethods().getDispatch().getMethodParameter());
    }

    @Override
    public List<RegisteredLoginMethod> provideRegisteredLoginMethods() {
        List<RegisteredLoginMethod> registered = new ArrayList<>();
        this.properties.getPassword().forEach((id, method) -> {
            if (method == null) {
                return;
            }
            registered.add(new RegisteredPasswordLoginMethod(id, name(id, method), method.isPrimary()));
        });
        return registered;
    }

    private String name(String id, BaseLoginMethodConfiguration method) {
        return StringUtils.hasText(method.getMethodName()) ? method.getMethodName() : id;
    }
}

