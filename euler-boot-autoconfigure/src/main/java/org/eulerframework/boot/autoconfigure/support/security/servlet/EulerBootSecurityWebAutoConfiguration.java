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

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityAuthenticationWebauthnProperties;
import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityProperties;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.web.endpoint.user.login.LoginMethodConfigDrivenContributor;
import org.eulerframework.security.web.endpoint.user.login.LoginMethodContributor;
import org.eulerframework.security.web.endpoint.user.login.LoginMethodHandler;
import org.eulerframework.security.web.endpoint.user.login.OtpLoginMethodHandler;
import org.eulerframework.security.web.endpoint.user.login.PasswordLoginMethodHandler;
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
        EulerBootSecurityProperties.class,
        EulerBootSecurityWebProperties.class,
        EulerBootSecurityAuthenticationWebauthnProperties.class,
        EulerBootSecurityWebEndpointProperties.class})
@ConditionalOnClass(EulerUserDetails.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        EulerBootWebSecurityConfiguration.class,
        EulerUserDetailsServiceConfiguration.class
})
public class EulerBootSecurityWebAutoConfiguration {

    /**
     * Generic contributor that turns the declared
     * {@code euler.security.login-method.*} entries into available
     * login methods by delegating each registration to the
     * {@link LoginMethodHandler} whose {@link LoginMethodHandler#type()}
     * matches.
     */
    @Bean
    @ConditionalOnMissingBean(name = "loginMethodConfigDrivenContributor")
    public LoginMethodConfigDrivenContributor loginMethodConfigDrivenContributor(
            List<LoginMethodHandler> handlers,
            EulerBootSecurityProperties securityProperties) {
        LoginMethodPropertiesMapper mapper = new LoginMethodPropertiesMapper(securityProperties);
        return new LoginMethodConfigDrivenContributor(handlers, mapper::asRegisteredLoginMethods);
    }

    @Bean
    @ConditionalOnMissingBean(PasswordLoginMethodHandler.class)
    public PasswordLoginMethodHandler passwordLoginMethodHandler(
            EulerBootSecurityWebEndpointProperties endpointProperties) {
        return new PasswordLoginMethodHandler(
                endpointProperties.getUser().getLoginProcessingUrl(),
                endpointProperties.getUser().getLoginPage(),
                endpointProperties.getLoginMethodDispatch().getMethodParameter());
    }

    @Bean
    @ConditionalOnMissingBean(OtpLoginMethodHandler.class)
    public OtpLoginMethodHandler otpLoginMethodHandler(
            EulerBootSecurityWebEndpointProperties endpointProperties) {
        return new OtpLoginMethodHandler(
                endpointProperties.getUser().getLoginPage(),
                endpointProperties.getLoginMethodDispatch().getMethodParameter());
    }
}
