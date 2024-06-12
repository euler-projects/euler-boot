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

import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.userdetails.*;
import org.eulerframework.security.core.userdetails.provisioning.*;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EulerUserDetails.class)
public class EulerUserDetailsServiceConfiguration {

    @Bean
    @ConditionalOnMissingBean(EulerUserDetailsManager.class)
    public ProviderEulerUserDetailsManager providerEulerUserDetailsManager(
            AuthenticationConfiguration authenticationConfiguration,
            EulerUserService eulerUserService,
            List<EulerUserDetailsProvider> eulerUserDetailsProviders) throws Exception {
        Assert.notNull(eulerUserService, "No eulerUserService bean was defined");
        Assert.notEmpty(eulerUserDetailsProviders, "At least one EulerUserDetailsProvider bean is required");
        ProviderEulerUserDetailsManager providerEulerUserDetailsManager = new ProviderEulerUserDetailsManager(eulerUserService, eulerUserDetailsProviders);
        providerEulerUserDetailsManager.setAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
        return providerEulerUserDetailsManager;
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        return new UsernamePasswordAuthenticationUserContext();
    }
}
