package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.UserContext;
import org.eulerframework.security.core.UserDetailsPrincipalUserContext;
import org.eulerframework.security.spring.userdetails.EulerUserDetailsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
@ConditionalOnBean(EulerUserService.class)
public class EulerUserDetailsServiceConfiguration {

    @Bean
    public EulerUserDetailsService eulerUserDetailsService(
            EulerUserService eulerUserService,
            EulerBootSecurityWebProperties eulerBootSecurityProperties
    ) {
        return new EulerUserDetailsService(
                eulerUserService,
                eulerBootSecurityProperties.isEnableEmailSignIn(),
                eulerBootSecurityProperties.isEnableMobileSignIn(),
                eulerBootSecurityProperties.getUserDetailsCacheExpireTime().toMillis());
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
        return new UserDetailsPrincipalUserContext();
    }
}
