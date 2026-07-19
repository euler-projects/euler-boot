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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.resource.EulerBootResourceServerAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.eulerframework.security.core.context.DelegatingUserContext;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.context.UserDetailsPrincipalUserContext;
import org.eulerframework.security.core.userdetails.EulerUserDetailsService;
import org.eulerframework.security.oauth2.server.authorization.userdetails.provider.AuthorizationServerUserDetailsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.eulerframework.security.oauth2.core.context.OAuth2AuthenticatedPrincipalUserContext;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

@AutoConfiguration(before = {
        // When the authorization server also acts as a resource server, ensure that
        // built-in resource server overrides take effect, including but not limited to:
        // - Activating OAuth2NativeTokenAuthenticationManager via OAuth2AuthorizationService bean detection
        // - Preferring the authorization server's UserContext
        EulerBootResourceServerAutoConfiguration.class,

        // Ensure the authorization server's Spring Security overrides take effect,
        // including but not limited to:
        // - Preferring the authorization server's UserContext
        EulerBootSecurityWebAutoConfiguration.class,
        EulerBootSecurityAutoConfiguration.class,

        // Supersede Spring's default authorization server auto-configuration
        OAuth2AuthorizationServerAutoConfiguration.class,
        // Supersede Spring's default resource server auto-configuration when
        // the authorization server also acts as a resource server
        OAuth2ResourceServerAutoConfiguration.class
})
@EnableConfigurationProperties({EulerBootAuthorizationServerProperties.class,
        EulerBootAuthorizationServerJwkProperties.class})
@ConditionalOnClass(OAuth2Authorization.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(EulerBootAuthorizationServerConfiguration.class)
public class EulerBootAuthorizationServerAutoConfiguration {
    @Bean
    public AuthorizationServerUserDetailsProvider authorizationServerUserDetailsProvider(
            EulerUserDetailsService eulerUserDetailsService) {
        return new AuthorizationServerUserDetailsProvider(eulerUserDetailsService);
    }

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        OAuth2AuthenticatedPrincipalUserContext oauth2AuthenticatedPrincipalUserContext =
                new OAuth2AuthenticatedPrincipalUserContext();
        UserDetailsPrincipalUserContext userDetailsPrincipalUserContext = new UserDetailsPrincipalUserContext();
        return new DelegatingUserContext(oauth2AuthenticatedPrincipalUserContext, userDetailsPrincipalUserContext);
    }
}
