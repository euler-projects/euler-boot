package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.eulerframework.security.oauth2.resource.OAuth2NativeTokenAuthenticationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(OAuth2AuthorizationService.class)
class NativeAuthorizationServiceResourceServerConfiguration {

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    @ConditionalOnBean(OAuth2AuthorizationService.class)
    SecurityFilterChain opaqueTokenSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationService authorizationService,
            EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
        AuthenticationManager authenticationManager = new OAuth2NativeTokenAuthenticationManager(authorizationService);

        http
                .securityMatcher(eulerBootResourceServerProperties.getUrlPatterns())
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/api/resourceOwnerAuthorities").hasAuthority("SCOPE_get-resource-owner-authority")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE))
                .oauth2ResourceServer(resourceServer -> resourceServer.authenticationManagerResolver(context ->
                        authenticationManager));
        return http.build();
    }
}
