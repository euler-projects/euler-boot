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
package org.eulerframework.boot.autoconfigure.support.security.factor;

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityAuthenticationFactorProperties;
import org.eulerframework.security.authentication.factor.DelegatingUserAuthenticationService;
import org.eulerframework.security.authentication.factor.UserAuthenticationService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for the user authentication factor module.
 * <p>
 * Wires up a single {@link DelegatingUserAuthenticationService} bean that
 * routes to every factor-specific {@link UserAuthenticationService} bean
 * registered by business code. The configuration itself is gated by
 * {@link ConditionalOnBean ConditionalOnBean(UserAuthenticationService)} so
 * that the delegating bean is <em>not</em> registered when no factor
 * implementation is available — this keeps the {@code /user/identities}
 * filter from being attached to the authorization-server filter chain.
 * <p>
 * The collected services list intentionally excludes any pre-existing
 * {@code DelegatingUserAuthenticationService} bean (defensive — Spring's
 * {@code ObjectProvider} would not include the bean currently being created
 * anyway) so that a custom user-defined delegating bean does not cause
 * recursion when one is provided.
 */
@AutoConfiguration
@ConditionalOnClass(UserAuthenticationService.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "euler.security.authentication-factor",
        name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(UserAuthenticationService.class)
@EnableConfigurationProperties(EulerBootSecurityAuthenticationFactorProperties.class)
public class EulerBootSecurityAuthenticationFactorAutoConfiguration {

    /**
     * Default delegating service collecting every factor-specific
     * {@link UserAuthenticationService} bean. Skipped when business code
     * provides its own {@link DelegatingUserAuthenticationService}.
     */
    @Bean
    @ConditionalOnMissingBean
    public DelegatingUserAuthenticationService delegatingUserAuthenticationService(
            ObjectProvider<UserAuthenticationService> serviceProvider) {
        List<UserAuthenticationService> services = serviceProvider.orderedStream()
                .filter(svc -> !(svc instanceof DelegatingUserAuthenticationService))
                .toList();
        return new DelegatingUserAuthenticationService(services);
    }
}
