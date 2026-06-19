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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.boot.autoconfigure.support.security.util.SecurityFilterUtils;
import org.eulerframework.security.oauth2.resource.OAuth2NativeTokenAuthenticationManager;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.util.StringUtils;

import static org.springframework.security.config.Customizer.withDefaults;

class EulerBootResourceServerConfiguration {
    static void applyCommonConfiguration(
            HttpSecurity http,
            EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
        String[] urlPatterns = eulerBootResourceServerProperties.getUrlPatterns();
        String[] ignoredUrlPatterns = eulerBootResourceServerProperties.getIgnoredUrlPatterns();
        SecurityFilterUtils.configSecurityMatcher(http, urlPatterns, ignoredUrlPatterns);

        http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());

        DefaultBearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();
        bearerTokenResolver.setAllowUriQueryParameter(true);
        http.oauth2ResourceServer(resourceServer -> resourceServer.bearerTokenResolver(bearerTokenResolver));
    }

    /**
     * Disables CSRF entirely. Suitable for pure stateless resource-server chains
     * that authenticate only via {@code Authorization: Bearer ...} and never
     * rely on cookie-based sessions.
     */
    private static void applyStatelessBearerCsrf(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
    }


    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
    @Conditional(KeyValueCondition.class)
    static class KeyValueJwtResourceServerConfiguration {
        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            EulerBootResourceServerConfiguration.applyStatelessBearerCsrf(http);
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
    static class JwkSetUriResourceServerConfiguration {

        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            EulerBootResourceServerConfiguration.applyStatelessBearerCsrf(http);
            http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService")
    @ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
    static class OpaqueTokenResourceServerConfiguration {

        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
            EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
            EulerBootResourceServerConfiguration.applyStatelessBearerCsrf(http);
            http.oauth2ResourceServer(resourceServer -> resourceServer.opaqueToken(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(OAuth2AuthorizationService.class)
    static class LocalAuthorizationServerResourceServerConfiguration {

        /**
         * Session-aware filter chain that accepts either an existing HTTP
         * session login (loaded by {@code SecurityContextHolderFilter}) or an
         * OAuth2 Bearer token resolved against the local
         * {@link OAuth2AuthorizationService}.
         *
         * <p>Activated only when {@code euler.security.web.enabled=true},
         * because the session half of the chain depends on the default web
         * security filter chain performing the actual form login that
         * populates the session. When web security is disabled, the
         * {@link Stateless} fallback is used instead.</p>
         *
         * <p>{@link SessionCreationPolicy#IF_REQUIRED} is required so the chain
         * still reads any pre-existing session populated by the default form
         * login filter chain, instead of the resource server's default
         * {@code STATELESS} policy which would discard it.</p>
         *
         * <p>CSRF protection is kept enabled using the shared
         * {@link CsrfTokenRepository} bean (aligned with the default web filter
         * chain) so cookie/session based requests are still protected. Requests
         * carrying an {@code Authorization} header are exempted, because
         * Bearer-token calls do not rely on ambient credentials and are not
         * vulnerable to CSRF.</p>
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(prefix = "euler.security.web", name = "enabled", havingValue = "true")
        static class SessionAware {
            @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
            @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
            @Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
            SecurityFilterChain resourceServerSecurityFilterChain(
                    HttpSecurity http,
                    OAuth2AuthorizationService authorizationService,
                    CsrfTokenRepository csrfTokenRepository,
                    EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
                EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
                http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
                http.csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .ignoringRequestMatchers(request -> request.getHeader("Authorization") != null));
                AuthenticationManager authenticationManager = new OAuth2NativeTokenAuthenticationManager(authorizationService);
                http.oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationManagerResolver(request -> authenticationManager));
                return http.build();
            }
        }

        /**
         * Stateless Bearer-only filter chain used when
         * {@code euler.security.web.enabled} is false or absent. Behaves the
         * same way as the other stateless resource-server chains
         * (JWK/OpaqueToken/KeyValueJwt): CSRF disabled, no session reliance.
         * Authentication is still delegated to the local
         * {@link OAuth2AuthorizationService} via
         * {@link OAuth2NativeTokenAuthenticationManager}.
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnProperty(prefix = "euler.security.web", name = "enabled", havingValue = "false", matchIfMissing = true)
        static class Stateless {
            @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
            @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
            @Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
            SecurityFilterChain resourceServerSecurityFilterChain(
                    HttpSecurity http,
                    OAuth2AuthorizationService authorizationService,
                    EulerBootResourceServerProperties eulerBootResourceServerProperties) throws Exception {
                EulerBootResourceServerConfiguration.applyCommonConfiguration(http, eulerBootResourceServerProperties);
                EulerBootResourceServerConfiguration.applyStatelessBearerCsrf(http);
                AuthenticationManager authenticationManager = new OAuth2NativeTokenAuthenticationManager(authorizationService);
                http.oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationManagerResolver(request -> authenticationManager));
                return http.build();
            }
        }
    }

    static class KeyValueCondition extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition("Public Key Value Condition");
            Environment environment = context.getEnvironment();
            String publicKeyLocation = environment
                    .getProperty("spring.security.oauth2.resourceserver.jwt.public-key-location");
            if (!StringUtils.hasText(publicKeyLocation)) {
                return ConditionOutcome.noMatch(message.didNotFind("public-key-location property").atAll());
            }
            String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
            if (StringUtils.hasText(jwkSetUri)) {
                return ConditionOutcome.noMatch(message.found("jwk-set-uri property").items(jwkSetUri));
            }
            String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
            if (StringUtils.hasText(issuerUri)) {
                return ConditionOutcome.noMatch(message.found("issuer-uri property").items(issuerUri));
            }
            return ConditionOutcome.match(message.foundExactly("public key location property"));
        }

    }
}
