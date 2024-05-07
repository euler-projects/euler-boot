package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.EulerAuthorizationServerConfiguration;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration(before = {
        EulerBootSecurityWebAutoConfiguration.class,
        OAuth2AuthorizationServerAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@ConditionalOnClass(EulerAuthorizationServerConfiguration.class)
public class EulerBootAuthorizationServerAutoConfiguration {

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    public SecurityFilterChain authorizationServerSecurityFilterChain(AuthenticationConfiguration authenticationConfiguration, HttpSecurity http) throws Exception {
        EulerAuthorizationServerConfiguration eulerAuthorizationServerConfiguration =
                new EulerAuthorizationServerConfiguration(authenticationConfiguration, http);
        return eulerAuthorizationServerConfiguration.authorizationServerSecurityFilterChain();
    }

}
