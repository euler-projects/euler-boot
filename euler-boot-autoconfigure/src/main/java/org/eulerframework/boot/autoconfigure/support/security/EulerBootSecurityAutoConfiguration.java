package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.spring.userdetails.EulerUserDetailsService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@AutoConfiguration
@ConditionalOnBean(EulerUserService.class)
@EnableConfigurationProperties({
        EulerBootSecurityProperties.class
})
public class EulerBootSecurityAutoConfiguration {
    @Bean
    public EulerUserDetailsService eulerUserDetailsService(
            EulerUserService eulerUserService,
            EulerBootSecurityProperties eulerBootSecurityProperties
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
        return new BCryptPasswordEncoder();
    }
}
