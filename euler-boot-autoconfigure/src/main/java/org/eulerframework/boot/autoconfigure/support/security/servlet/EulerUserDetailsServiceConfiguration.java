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

import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.userdetails.DelegatingEulerUserDetailsManager;
import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.eulerframework.security.core.userdetails.EulerUserDetailsProvider;
import org.eulerframework.security.core.userdetails.DefaultEulerUserDetailsManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EulerUserDetails.class)
@ConditionalOnBean(EulerUserDetailsProvider.class)
public class EulerUserDetailsServiceConfiguration {

    @Bean
    public DelegatingEulerUserDetailsManager eulerUserDetailsService(
            List<EulerUserDetailsProvider> eulerUserDetailsProviders
    ) {
        DefaultEulerUserDetailsManager defaultEulerUserDetailsManager = new DefaultEulerUserDetailsManager(eulerUserDetailsProviders);
        DelegatingEulerUserDetailsManager delegatingEulerUserDetailsManager = new DelegatingEulerUserDetailsManager();
        delegatingEulerUserDetailsManager.setEulerUserDetailsManagers(Collections.singletonList(defaultEulerUserDetailsManager));
        return delegatingEulerUserDetailsManager;
    }

    @Bean
    public AuthenticationManager authenticationManager(List<AuthenticationProvider> providers) {
        return new ProviderManager(providers);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        return daoAuthenticationProvider;
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
