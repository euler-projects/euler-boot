package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.eulerframework.security.core.userdetails.EulerUserDetailsProvider;
import org.eulerframework.security.core.userdetails.EulerUserDetailsService;
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

import java.util.List;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EulerUserDetailsService.class)
public class EulerUserDetailsServiceConfiguration {

    @Bean
    public EulerUserDetailsService eulerUserDetailsService(
            List<EulerUserDetailsProvider> eulerUserDetailsProviders
    ) {
        return new EulerUserDetailsService(eulerUserDetailsProviders);
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
