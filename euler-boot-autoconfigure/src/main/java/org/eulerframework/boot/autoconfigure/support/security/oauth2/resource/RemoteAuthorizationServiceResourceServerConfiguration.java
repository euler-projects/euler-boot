package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
class RemoteAuthorizationServiceResourceServerConfiguration {
    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
    SecurityFilterChain opaqueTokenSecurityFilterChain(
            HttpSecurity http,
            EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
        http
                .securityMatcher(eulerBootResourceServerProperties.getUrlPatterns())
                .authorizeHttpRequests((requests) -> requests.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE))
                .oauth2ResourceServer(resourceServer -> resourceServer.opaqueToken(withDefaults()));
        return http.build();
    }

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    SecurityFilterChain jwtSecurityFilterChain(
            HttpSecurity http,
            EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
        http
                .securityMatcher(eulerBootResourceServerProperties.getUrlPatterns())
                .authorizeHttpRequests((requests) -> requests.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()));
        return http.build();
    }
}
