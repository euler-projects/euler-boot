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

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityAutoConfiguration;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.web.endpoint.user.login.LoginMethodConfigDrivenContributor;
import org.eulerframework.security.web.endpoint.user.login.LoginMethodContributor;
import org.eulerframework.security.web.endpoint.user.login.LoginMethodTypeHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

@AutoConfiguration(
        before = {
                EulerBootSecurityAutoConfiguration.class
        },
        beforeName = {
                "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
                "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
                // Suppress the default SecurityFilterChain that
                // ServletWebSecurityAutoConfiguration.SecurityFilterChainConfiguration
                // would otherwise contribute.
                "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration",
                // Same reason for the OAuth2-client servlet default
                // chain contributed by
                // OAuth2ClientWebSecurityAutoConfiguration.OAuth2SecurityFilterChainConfiguration.
                "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration"
        }
)
@EnableConfigurationProperties({
        EulerBootSecurityWebProperties.class,
        EulerBootSecurityWebAuthnProperties.class,
        EulerBootSecurityWebEndpointProperties.class})
@ConditionalOnClass(EulerUserDetails.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        EulerBootWebSecurityConfiguration.class,
        EulerUserDetailsServiceConfiguration.class
})
public class EulerBootSecurityWebAutoConfiguration {

    /**
     * Generic dispatcher that turns
     * {@code euler.security.web.login-methods.*} entries into
     * {@link org.eulerframework.security.web.endpoint.user.login.LoginMethodView}s
     * on the shared login page by delegating each entry to the
     * {@link LoginMethodTypeHandler} whose {@link LoginMethodTypeHandler#type()}
     * matches. The per-type handler beans themselves are contributed
     * by their respective feature modules (e.g.
     * {@code EulerBootOAuth2ClientAutoConfiguration} for
     * {@code type: oauth2}); this bean is the cross-type glue.
     *
     * <p>Registered here rather than in per-feature autoconfigs because
     * the dispatcher is a servlet-web concern (it feeds the login
     * page's {@code loginMethods} model attribute) and remains useful
     * even when no feature-specific handler is present &mdash; the
     * resulting list is simply empty.
     */
    @Bean
    @ConditionalOnMissingBean(name = "loginMethodConfigDrivenContributor")
    public LoginMethodContributor loginMethodConfigDrivenContributor(
            List<LoginMethodTypeHandler> handlers,
            EulerBootSecurityWebProperties webProperties) {
        return new LoginMethodConfigDrivenContributor(handlers, webProperties::getLoginMethods);
    }
}
