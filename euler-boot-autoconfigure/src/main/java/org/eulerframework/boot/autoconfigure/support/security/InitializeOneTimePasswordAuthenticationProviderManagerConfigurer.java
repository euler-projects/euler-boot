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
package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.security.authentication.otp.OneTimePasswordAuthenticationProvider;
import org.eulerframework.security.authentication.otp.OtpTicketService;
import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.provisioning.jit.JitProvisioningPolicyResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

/**
 * Registers {@link OneTimePasswordAuthenticationProvider} with the shared
 * {@link org.springframework.security.authentication.AuthenticationManager
 * AuthenticationManager} when the OTP module is enabled and its required
 * beans are present.
 * <p>
 * Applications may supply their own {@link OneTimePasswordAuthenticationProvider}
 * bean to replace the default one assembled from the OTP SPI beans.
 */
@Order(InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.DEFAULT_ORDER)
public class InitializeOneTimePasswordAuthenticationProviderManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
    static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 3000;

    private final ApplicationContext context;

    InitializeOneTimePasswordAuthenticationProviderManagerConfigurer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(AuthenticationManagerBuilder auth) {
        auth.apply(new InitializeOtpManagerConfigurer());
    }

    class InitializeOtpManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void configure(AuthenticationManagerBuilder auth) {
            EulerSecurityAuthenticationOtpProperties properties = InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.this.context
                    .getBeanProvider(EulerSecurityAuthenticationOtpProperties.class)
                    .getIfAvailable();
            if (properties == null || !properties.isEnabled()) {
                return;
            }

            // Prefer a user-supplied provider bean; otherwise assemble the
            // default one from the OTP SPI beans.
            OneTimePasswordAuthenticationProvider provider = InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.this.context
                    .getBeanProvider(OneTimePasswordAuthenticationProvider.class)
                    .getIfAvailable();
            if (provider != null) {
                auth.authenticationProvider(provider);
                return;
            }

            OtpTicketService otpTicketService = InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.this.context
                    .getBeanProvider(OtpTicketService.class)
                    .getIfAvailable();
            UserIdentityService userIdentityService = InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.this.context
                    .getBeanProvider(UserIdentityService.class)
                    .getIfAvailable();
            EulerUserService eulerUserService = InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.this.context
                    .getBeanProvider(EulerUserService.class)
                    .getIfAvailable();
            JitProvisioningPolicyResolver jitProvisioningPolicyResolver = InitializeOneTimePasswordAuthenticationProviderManagerConfigurer.this.context
                    .getBeanProvider(JitProvisioningPolicyResolver.class)
                    .getIfAvailable();
            if (otpTicketService == null
                    || userIdentityService == null
                    || eulerUserService == null
                    || jitProvisioningPolicyResolver == null) {
                // Required SPI beans missing: skip registration. The OAuth2
                // grant path fails fast on its own when it is enabled.
                return;
            }

            auth.authenticationProvider(new OneTimePasswordAuthenticationProvider(
                    otpTicketService, userIdentityService, eulerUserService, jitProvisioningPolicyResolver));
        }
    }
}
