package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.apache.commons.lang3.ArrayUtils;
import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.security.oauth2.resource.OAuth2NativeTokenAuthenticationManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.KeyValueCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

class EulerBootResourceServerConfiguration {
    static void applyCommonConfiguration(
            HttpSecurity http,
            EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
        if (!ArrayUtils.isEmpty(eulerBootResourceServerProperties.getUrlPatterns())) {
            http.securityMatcher(eulerBootResourceServerProperties.getUrlPatterns());
        }

        http
                .authorizeHttpRequests((requests) -> requests.anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers(AnyRequestMatcher.INSTANCE));
    }


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
    static class KeyValueJwtResourceServerConfiguration {

        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
        @Conditional(KeyValueCondition.class)
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
    static class JwkSetUriResourceServerConfiguration {

        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
        @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
    static class OpaqueTokenResourceServerConfiguration {

        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
        @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            http.oauth2ResourceServer(resourceServer -> resourceServer.opaqueToken(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(OAuth2AuthorizationService.class)
    static class LocalAuthorizationServerResourceServerConfiguration {

        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
        @ConditionalOnBean(OAuth2AuthorizationService.class)
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                OAuth2AuthorizationService authorizationService,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            AuthenticationManager authenticationManager = new OAuth2NativeTokenAuthenticationManager(authorizationService);

            if (!ArrayUtils.isEmpty(eulerBootResourceServerProperties.getUrlPatterns())) {
                http.securityMatcher(eulerBootResourceServerProperties.getUrlPatterns());
            }

            http.oauth2ResourceServer(resourceServer -> resourceServer.authenticationManagerResolver(context -> authenticationManager));
            return http.build();
        }
    }
}
