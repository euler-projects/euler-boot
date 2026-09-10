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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.eulerframework.boot.autoconfigure.support.security.EulerSecurityAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerSecurityWebAutoConfiguration;
import org.eulerframework.security.core.context.DelegatingUserContext;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.context.UserDetailsPrincipalUserContext;
import org.eulerframework.security.oauth2.core.context.OAuth2AuthenticatedPrincipalUserContext;
import org.eulerframework.security.oauth2.resource.authorization.userdetails.provider.ResourceServerUserDetailsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

@AutoConfiguration(
        before = {
                // Ensure the resource server's Spring Security overrides take effect,
                // including but not limited to:
                // - Preferring the resource server's UserContext
                EulerSecurityWebAutoConfiguration.class,
                EulerSecurityAutoConfiguration.class,

                // Supersede Spring's default resource server auto-configuration
                OAuth2ResourceServerAutoConfiguration.class
        })
@EnableConfigurationProperties(EulerResourceServerProperties.class)
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        EulerResourceServerSecurityConfiguration.LocalAuthorizationServerResourceServerConfiguration.class,
        EulerResourceServerSecurityConfiguration.OpaqueTokenResourceServerConfiguration.class,
        EulerResourceServerSecurityConfiguration.JwkSetUriResourceServerConfiguration.class,
        EulerResourceServerSecurityConfiguration.KeyValueJwtResourceServerConfiguration.class
})
public class EulerResourceServerAutoConfiguration {
    @Bean
    public ResourceServerUserDetailsProvider resourceServerUserDetailsProvider() {
        return new ResourceServerUserDetailsProvider();
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
