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

import jakarta.annotation.Nonnull;
import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.userdetails.*;
import org.eulerframework.security.core.userdetails.provisioning.*;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EulerUserDetails.class)
public class EulerUserDetailsServiceConfiguration {

    @Bean
    @Conditional(DefaultEulerUserDetailsManagerCondition.class)
    @ConditionalOnMissingBean(EulerUserDetailsManager.class)
    public DelegateEulerUserDetailsManagerSupplier<DefaultEulerUserDetails, MultiProviderEulerUserDetailsManager>
    multiProviderEulerUserDetailsManagerSupplier(
            AuthenticationConfiguration authenticationConfiguration,
            EulerUserService eulerUserService,
            List<EulerUserDetailsProvider> eulerUserDetailsProviders) throws Exception {
        Assert.notNull(eulerUserService, "No eulerUserService bean was defined");
        Assert.notEmpty(eulerUserDetailsProviders, "At least one EulerUserDetailsProvider bean is required");
        MultiProviderEulerUserDetailsManager multiProviderEulerUserDetailsManager = new MultiProviderEulerUserDetailsManager(eulerUserService, eulerUserDetailsProviders);
        multiProviderEulerUserDetailsManager.setAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
        return new DelegateEulerUserDetailsManagerSupplier<>(DefaultEulerUserDetails.class, multiProviderEulerUserDetailsManager);
    }

    @Bean
    @ConditionalOnBean(DelegateEulerUserDetailsManagerSupplier.class)
    @ConditionalOnMissingBean(EulerUserDetailsManager.class)
    public EulerUserDetailsManager eulerUserDetailsManager(
            List<DelegateEulerUserDetailsManagerSupplier<? extends UserDetails, ? extends EulerUserDetailsManager>> delegateEulerUserDetailsManagerSuppliers
    ) {
        if (delegateEulerUserDetailsManagerSuppliers.size() == 1) {
            return delegateEulerUserDetailsManagerSuppliers.get(0).get();
        } else {
            LinkedHashMap<Class<? extends UserDetails>, EulerUserDetailsManager> eulerUserDetailsManagers = new LinkedHashMap<>();

            for (DelegateEulerUserDetailsManagerSupplier<? extends UserDetails, ? extends EulerUserDetailsManager> managerSupplier : delegateEulerUserDetailsManagerSuppliers) {
                Class<? extends UserDetails> supportType = managerSupplier.getUserDetailsType();
                if (eulerUserDetailsManagers.containsKey(supportType)) {
                    throw new BeanInitializationException("There are more than one EulerUserDetailsManager bean for user details type: " + supportType);
                }
                EulerUserDetailsManager eulerUserDetailsManager = managerSupplier.get();
                eulerUserDetailsManagers.put(supportType, eulerUserDetailsManager);
            }

            return new DelegatingEulerUserDetailsManager(eulerUserDetailsManagers);
        }
    }

    @Bean
    @ConditionalOnMissingBean(PasswordEncoder.class)
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(
            UserDetailsService userDetailsService,
            @Autowired(required = false) UserDetailsPasswordService userDetailsPasswordService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(passwordEncoder);
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setUserDetailsPasswordService(userDetailsPasswordService);
        return daoAuthenticationProvider;
    }

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        return new UsernamePasswordAuthenticationUserContext();
    }

    static class DefaultEulerUserDetailsManagerCondition implements Condition {
        @Override
        public boolean matches(@Nonnull ConditionContext context, @Nonnull AnnotatedTypeMetadata metadata) {
            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
            String[] eulerUserDetailsManagerSupplierBeanNames = Objects.requireNonNull(beanFactory)
                    .getBeanNamesForType(DelegateEulerUserDetailsManagerSupplier.class);

            for (String beanName : eulerUserDetailsManagerSupplierBeanNames) {
                DelegateEulerUserDetailsManagerSupplier<?, ?> delegateEulerUserDetailsManagerSupplier =
                        beanFactory.getBean(beanName, DelegateEulerUserDetailsManagerSupplier.class);
                if (DefaultEulerUserDetails.class.isAssignableFrom(delegateEulerUserDetailsManagerSupplier.getUserDetailsType())) {
                    return false;
                }
            }

            return true;
        }
    }
}
