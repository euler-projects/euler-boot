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
package org.eulerframework.boot.autoconfigure.support.security.login;

import org.eulerframework.boot.autoconfigure.support.security.login.oauth2.EulerSecurityLoginMethodOAuth2Configuration;
import org.eulerframework.boot.autoconfigure.support.security.login.otp.EulerSecurityLoginMethodOtpConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.login.password.EulerSecurityLoginMethodPasswordConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.login.password.EulerSecurityLoginMethodPasswordProperties;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.eulerframework.security.web.login.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * Autoconfiguration assembling the login method registry: it collects
 * what every {@link LoginMethodConfiguration} declares into a single
 * {@link RegisteredLoginMethodRepository}, and publishes the
 * {@link LoginMethodService} that serves it.
 *
 * <p>The framework's own types are configured by the imported classes,
 * each self-contained; a type outside the framework joins the same way,
 * by publishing a {@link LoginMethodConfiguration} bean of its own.
 */
@AutoConfiguration(
        // make sure LoginMethodService has bean created
        // before EulerSecurityUserEndpoint
        before = EulerBootSecurityWebAutoConfiguration.class,
        // Wait for Spring Boot to publish the default
        // ClientRegistrationRepository, which the oauth2 type's handler
        // is conditional on. Named rather than typed: OAuth2 client
        // support is an optional dependency.
        afterName = "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration"
)
@Import({
        EulerSecurityLoginMethodPasswordConfiguration.class,
        EulerSecurityLoginMethodOAuth2Configuration.class,
        EulerSecurityLoginMethodOtpConfiguration.class
})
public class EulerSecurityLoginMethodAutoConfiguration {

    /**
     * The registry every declared login method is served out of,
     * populated once from the contributions of every
     * {@link LoginMethodConfiguration} in the context.
     *
     * <p>Publishing a {@link RegisteredLoginMethodRepository} bean
     * elsewhere replaces this one, and with it the configuration-driven
     * declarations altogether: a repository serving login methods out of
     * a database owns the full set.
     *
     * @throws IllegalArgumentException if two declarations share an id
     *                                  or the name clients address them
     *                                  by
     */
    @Bean
    @ConditionalOnMissingBean(RegisteredLoginMethodRepository.class)
    public RegisteredLoginMethodRepository registeredLoginMethodRepository(
            List<LoginMethodConfiguration> loginMethodConfigurations) {
        RegisteredLoginMethodRepository repository = new DefaultRegisteredLoginMethodRepository();
        for (LoginMethodConfiguration loginMethodConfiguration : loginMethodConfigurations) {
            loginMethodConfiguration.provideRegisteredLoginMethods()
                    .forEach(repository::save);
        }
        return repository;
    }

    /**
     * Serves the registered login methods, projecting each one through
     * the {@link LoginMethodHandler} that matches its type.
     */
    @Bean
    @ConditionalOnMissingBean(LoginMethodService.class)
    public LoginMethodService loginMethodService(
            List<LoginMethodHandler> loginMethodHandlers,
            RegisteredLoginMethodRepository registeredLoginMethodRepository) {
        return new DefaultLoginMethodService(loginMethodHandlers, registeredLoginMethodRepository);
    }
}
