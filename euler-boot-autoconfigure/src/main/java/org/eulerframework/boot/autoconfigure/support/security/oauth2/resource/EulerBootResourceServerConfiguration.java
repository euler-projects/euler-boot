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
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
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
         * Stateless Bearer-only filter chain for resource endpoints colocated
         * with the local OAuth2 Authorization Server.
         *
         * <p>Authentication is delegated to
         * {@link OAuth2NativeTokenAuthenticationManager}, which resolves
         * Bearer tokens directly against the in-process
         * {@link OAuth2AuthorizationService}. Because the resource server and
         * the authorization server share the same authorization store, token
         * validation is a local lookup &mdash; this chain does <em>not</em>
         * fetch a JWK set and does <em>not</em> call an introspection
         * endpoint, avoiding the network round-trip that the sibling
         * {@link JwkSetUriResourceServerConfiguration} and
         * {@link OpaqueTokenResourceServerConfiguration} chains incur.</p>
         *
         * <p>The chain is fully stateless: CSRF is disabled and no HTTP
         * session is read or created. Cookie/session-based authentication,
         * when required, must be supplied by a separate filter chain rather
         * than mixed into this one.</p>
         */
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
