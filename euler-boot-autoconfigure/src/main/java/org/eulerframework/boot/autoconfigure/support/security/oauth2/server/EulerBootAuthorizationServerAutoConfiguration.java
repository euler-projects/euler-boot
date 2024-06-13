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
import org.eulerframework.security.oauth2.server.authorization.userdetails.provisioning.AuthorizationServerUserDetailsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.context.OAuth2AuthenticatedPrincipalUserContext;
import org.springframework.security.oauth2.core.userdetails.provisioning.OAuth2TokenUserDetailsProvider;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.util.List;

@AutoConfiguration(before = {
        EulerBootResourceServerAutoConfiguration.class,
        EulerBootSecurityWebAutoConfiguration.class,
        EulerBootSecurityAutoConfiguration.class,
        OAuth2AuthorizationServerAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@EnableConfigurationProperties(EulerBootAuthorizationServerProperties.class)
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
    public UserContext userContext(List<OAuth2TokenUserDetailsProvider> tokenUserDetailsProviders) {
        OAuth2AuthenticatedPrincipalUserContext oauth2AuthenticatedPrincipalUserContext =
                new OAuth2AuthenticatedPrincipalUserContext(tokenUserDetailsProviders);
        UserDetailsPrincipalUserContext userDetailsPrincipalUserContext = new UserDetailsPrincipalUserContext();
        return new DelegatingUserContext(oauth2AuthenticatedPrincipalUserContext, userDetailsPrincipalUserContext);
    }
}
