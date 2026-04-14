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

import org.eulerframework.security.authentication.apple.AppleAppAttestAssertionAuthenticationProvider;
import org.eulerframework.security.authentication.apple.AppleAppAttestValidationService;
import org.eulerframework.security.core.userdetails.EulerAppleAppAttestUserDetailsService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

@Order(InitializeAppleAppAttestBeanManagerConfigurer.DEFAULT_ORDER)
public class InitializeAppleAppAttestBeanManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
    static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 4000;

    private final ApplicationContext context;

    InitializeAppleAppAttestBeanManagerConfigurer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(AuthenticationManagerBuilder auth) {
        auth.apply(new InitializeAppleAppAttestManagerConfigurer());
    }

    class InitializeAppleAppAttestManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void configure(AuthenticationManagerBuilder auth) {
            String[] userDetailsServiceBeanNames = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBeanNamesForType(EulerAppleAppAttestUserDetailsService.class);
            if (userDetailsServiceBeanNames.length == 0) {
                return;
            }

            EulerBootSecurityAppAttestProperties properties = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBean(EulerBootSecurityAppAttestProperties.class);

            if (!properties.isEnabled()) {
                return;
            }

            EulerAppleAppAttestUserDetailsService userDetailsService = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBean(userDetailsServiceBeanNames[0], EulerAppleAppAttestUserDetailsService.class);

            AppleAppAttestValidationService validationService = getOrCreateValidationService();

            // Register assertion (device re-authentication) provider only.
            // Attestation (device registration) is now handled by AppAttestRegistrationAuthenticationProvider
            // via AppAttestSecurityConfigurer, outside the AuthenticationManager.
            AppleAppAttestAssertionAuthenticationProvider assertionProvider = new AppleAppAttestAssertionAuthenticationProvider(
                    validationService, userDetailsService);
            auth.authenticationProvider(assertionProvider);
        }

        private AppleAppAttestValidationService getOrCreateValidationService() {
            String[] validationServiceBeanNames = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBeanNamesForType(AppleAppAttestValidationService.class);
            if (validationServiceBeanNames.length > 0) {
                return InitializeAppleAppAttestBeanManagerConfigurer.this.context
                        .getBean(validationServiceBeanNames[0], AppleAppAttestValidationService.class);
            }

            throw new IllegalStateException(
                    "No AppleAppAttestValidationService bean found. " +
                    "Ensure App Attest auto-configuration is active or provide a custom bean.");
        }
    }
}
