package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@AutoConfiguration(
        before = {
                EulerBootSecurityWebAutoConfiguration.class
        },
        after = {
                OAuth2ResourceServerAutoConfiguration.class,
        })
public class EulerBootResourceServerAutoConfiguration {
    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnBean(OpaqueTokenIntrospector.class)
    SecurityFilterChain opaqueTokenSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests((requests) -> requests.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE))
                .oauth2ResourceServer((resourceServer) -> resourceServer.opaqueToken(withDefaults()));
        return http.build();
    }

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnBean(JwtDecoder.class)
    SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests((requests) -> requests.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE))
                .oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
        return http.build();
    }
}
