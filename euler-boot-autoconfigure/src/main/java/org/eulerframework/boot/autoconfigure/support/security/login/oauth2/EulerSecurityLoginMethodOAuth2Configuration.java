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
package org.eulerframework.boot.autoconfigure.support.security.login.oauth2;

import org.eulerframework.boot.autoconfigure.support.security.login.BaseLoginMethodConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.login.LoginMethodConfiguration;
import org.eulerframework.security.oauth2.client.web.OAuth2LoginMethodHandler;
import org.eulerframework.security.web.login.RegisteredLoginMethod;
import org.eulerframework.security.web.login.RegisteredOAuth2LoginMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures the {@code oauth2} login method type: sign-in delegated to
 * an external identity provider.
 *
 * <p>OAuth2 client support is an optional dependency, so this type stays
 * out of the context entirely when it is absent. When it is present but
 * no {@link ClientRegistrationRepository} is, the handler is not
 * published either: the declarations are then left unserved, which the
 * login method service reports and skips rather than the application
 * failing to start.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({OAuth2LoginMethodHandler.class, ClientRegistrationRepository.class})
@EnableConfigurationProperties({EulerSecurityLoginMethodOAuth2Properties.class})
public class EulerSecurityLoginMethodOAuth2Configuration implements LoginMethodConfiguration {

    private final EulerSecurityLoginMethodOAuth2Properties properties;

    public EulerSecurityLoginMethodOAuth2Configuration(EulerSecurityLoginMethodOAuth2Properties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnBean(ClientRegistrationRepository.class)
    @ConditionalOnMissingBean(OAuth2LoginMethodHandler.class)
    public OAuth2LoginMethodHandler oauth2LoginMethodHandler(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new OAuth2LoginMethodHandler(clientRegistrationRepository);
    }

    @Override
    public List<RegisteredLoginMethod> provideRegisteredLoginMethods() {
        List<RegisteredLoginMethod> registered = new ArrayList<>();
        this.properties.getOauth2().forEach((id, method) -> {
            if (method == null) {
                return;
            }
            registered.add(new RegisteredOAuth2LoginMethod(id, name(id, method),
                    method.getIdentityType(), method.isPrimary(), method.getProvider(),
                    method.getOauthClientRegistrationId()));
        });
        return registered;
    }

    private String name(String id, BaseLoginMethodConfiguration method) {
        return StringUtils.hasText(method.getMethodName()) ? method.getMethodName() : id;
    }
}
