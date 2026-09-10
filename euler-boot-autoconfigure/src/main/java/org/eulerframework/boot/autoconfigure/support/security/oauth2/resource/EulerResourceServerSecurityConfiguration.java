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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

class EulerResourceServerSecurityConfiguration {
    static void applyCommonConfiguration(
            HttpSecurity http,
            EulerResourceServerProperties eulerResourceServerProperties) throws Exception {
        String[] urlPatterns = eulerResourceServerProperties.getUrlPatterns();
        String[] ignoredUrlPatterns = eulerResourceServerProperties.getIgnoredUrlPatterns();
        SecurityFilterUtils.configSecurityMatcher(http, urlPatterns, ignoredUrlPatterns);

        http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());

        DefaultBearerTokenResolver bearerTokenResolver = new DefaultBearerTokenResolver();
        bearerTokenResolver.setAllowUriQueryParameter(true);
        http.oauth2ResourceServer(resourceServer -> resourceServer.bearerTokenResolver(bearerTokenResolver));
    }

    /**
     * CSRF policy for the resource-server chains, keyed on whether the
     * deployment can establish a session at all: with no shared
     * {@link CsrfTokenRepository} bean there is no session-creating chain
     * beside these, so no cookie-borne credential exists to protect and
     * CSRF stays disabled (pure stateless deployments). With one - the
     * hybrid case, where the default web chain logs browsers in - CSRF is
     * required exactly from callers riding that session, while Bearer
     * callers stay exempt; see {@link SessionAwareCsrfProtectionMatcher}.
     * The {@code sessionAware} flag overrides both cases: when it is off the
     * chains are strict Bearer-only and CSRF is disabled unconditionally.
     */
    private static void applyCsrf(
            HttpSecurity http, CsrfTokenRepository csrfTokenRepository, boolean sessionAware) throws Exception {
        if (!sessionAware || csrfTokenRepository == null) {
            http.csrf(AbstractHttpConfigurer::disable);
            return;
        }
        http.csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .requireCsrfProtectionMatcher(new SessionAwareCsrfProtectionMatcher()));
    }

    /**
     * Closes the session half of the dual credential model when
     * {@code session-aware} is off: the default
     * {@code SecurityContextRepository} would otherwise restore a
     * session-derived SecurityContext on every request (see the chain
     * Javadoc), so a request-scoped repository replaces it and the chains
     * authenticate exclusively from the Bearer token they resolve.
     */
    private static void applySessionAwareness(
            HttpSecurity http, EulerResourceServerProperties properties) throws Exception {
        if (properties.isSessionAware()) {
            return;
        }
        http.securityContext(securityContext -> securityContext
                .securityContextRepository(new RequestAttributeSecurityContextRepository()));
    }

    /**
     * Requires a CSRF token exactly from callers that ride an HTTP session.
     *
     * <p>The evaluation point is what makes this decidable: the
     * {@code SecurityContextHolderFilter} restores a session-borne
     * SecurityContext before the {@code CsrfFilter} runs, while the
     * {@code BearerTokenAuthenticationFilter} resolves its token only
     * afterwards. A non-anonymous context at this point therefore proves a
     * session credential. A Bearer credential is proved by the request
     * itself - the {@code Authorization} header, or the {@code access_token}
     * query parameter mirroring
     * {@link DefaultBearerTokenResolver#setAllowUriQueryParameter(boolean)} -
     * and is exempt: a header-borne token is immune to cross-site request
     * forgery by construction and could not carry a cookie-bound CSRF token
     * across sites anyway.
     */
    static final class SessionAwareCsrfProtectionMatcher implements RequestMatcher {
        private static final Set<String> CSRF_EXEMPT_METHODS = Set.of("GET", "HEAD", "TRACE", "OPTIONS");

        @Override
        public boolean matches(HttpServletRequest request) {
            if (CSRF_EXEMPT_METHODS.contains(request.getMethod())) {
                return false;
            }
            // Bearer is tested BEFORE the session on purpose, mirroring the
            // exclusivity of BearerTokenAuthenticationFilter downstream: once
            // a request carries a Bearer token, that filter owns the outcome
            // either way - a valid token replaces the session-derived
            // SecurityContext with the Bearer identity, an invalid one clears
            // the context and rejects the request on the spot - so the session
            // can never govern a request that carries a token. Exempting CSRF
            // on mere presence therefore matches the credential that will
            // actually take effect, and an invalid-token request dies before
            // any state change, losing nothing to the exemption. Testing the
            // session first would instead tax dual-credential callers (session
            // cookie plus valid Bearer) with a CSRF token although their
            // effective credential is the CSRF-immune Bearer one. Two
            // independent layers keep an invalid-token request away from the
            // session credential: the default failure handler terminates the
            // request outright, and even a hypothetical handler that continued
            // the chain would face an empty SecurityContext - the clearContext()
            // in the same catch block erases the session-derived authentication
            // first - so the request could only proceed as anonymous and be
            // rejected at the authorization step.
            if (carriesBearerToken(request)) {
                return false;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken);
        }

        private static boolean carriesBearerToken(HttpServletRequest request) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return true;
            }
            return request.getParameter("access_token") != null;
        }
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
                ObjectProvider<CsrfTokenRepository> csrfTokenRepositories,
                EulerResourceServerProperties eulerResourceServerProperties) throws Exception {
            EulerResourceServerSecurityConfiguration.applyCommonConfiguration(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applySessionAwareness(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applyCsrf(http, csrfTokenRepositories.getIfAvailable(),
                    eulerResourceServerProperties.isSessionAware());
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
                ObjectProvider<CsrfTokenRepository> csrfTokenRepositories,
                EulerResourceServerProperties eulerResourceServerProperties) throws Exception {
            EulerResourceServerSecurityConfiguration.applyCommonConfiguration(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applySessionAwareness(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applyCsrf(http, csrfTokenRepositories.getIfAvailable(),
                    eulerResourceServerProperties.isSessionAware());
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
                ObjectProvider<CsrfTokenRepository> csrfTokenRepositories,
                EulerResourceServerProperties eulerResourceServerProperties) throws Exception {
            EulerResourceServerSecurityConfiguration.applyCommonConfiguration(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applySessionAwareness(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applyCsrf(http, csrfTokenRepositories.getIfAvailable(),
                    eulerResourceServerProperties.isSessionAware());
            http.oauth2ResourceServer(resourceServer -> resourceServer.opaqueToken(withDefaults()));
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(OAuth2AuthorizationService.class)
    static class LocalAuthorizationServerResourceServerConfiguration {

        /**
         * Filter chain for resource endpoints colocated with the local
         * OAuth2 Authorization Server, accepting Bearer tokens and - where a
         * session-creating chain exists - existing sessions alike.
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
         * <p>The chain never creates or writes an HTTP session, but it is not
         * session-blind: the default {@code SecurityContextRepository}
         * delegates to {@code HttpSessionSecurityContextRepository}, so a
         * SecurityContext already stored in a session cookie - typically by
         * the default web filter chain - is restored and honoured here. The
         * CSRF policy follows the credential in play: Bearer callers are
         * exempt, session-borne callers must present a valid token, and in
         * pure stateless deployments - no shared {@code CsrfTokenRepository}
         * bean, no session-creating chain - CSRF stays disabled altogether
         * (see {@code applyCsrf} and {@link SessionAwareCsrfProtectionMatcher}).
         * The {@code euler.security.oauth2.resourceserver.session-aware}
         * switch (off by default) governs this session half: deployments that
         * opt in get the dual model above, while the default reduces these
         * chains to strict Bearer-only - refusing to restore a session-derived
         * SecurityContext and disabling CSRF - even in a hybrid deployment
         * (see {@code applySessionAwareness}).</p>
         */
        @Bean(SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.RESOURCE_SERVER_SECURITY_FILTER_CHAIN)
        @Order(SecurityFilterProperties.BASIC_AUTH_ORDER - 1)
        SecurityFilterChain resourceServerSecurityFilterChain(
                HttpSecurity http,
                OAuth2AuthorizationService authorizationService,
                ObjectProvider<CsrfTokenRepository> csrfTokenRepositories,
                EulerResourceServerProperties eulerResourceServerProperties) throws Exception {
            EulerResourceServerSecurityConfiguration.applyCommonConfiguration(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applySessionAwareness(http, eulerResourceServerProperties);
            EulerResourceServerSecurityConfiguration.applyCsrf(http, csrfTokenRepositories.getIfAvailable(),
                    eulerResourceServerProperties.isSessionAware());
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
