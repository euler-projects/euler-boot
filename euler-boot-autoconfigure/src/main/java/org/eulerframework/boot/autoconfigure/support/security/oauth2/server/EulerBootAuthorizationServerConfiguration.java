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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.security.core.context.DelegatingUserContext;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.oauth2.server.authorization.EulerRedisOAuth2AuthorizationConsentService;
import org.eulerframework.security.oauth2.server.authorization.EulerRedisOAuth2AuthorizationService;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.eulerframework.security.oauth2.resource.context.BearerTokenAuthenticationUserContext;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.EulerAuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.time.Duration;
import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EulerAuthorizationServerConfiguration.class)
public class EulerBootAuthorizationServerConfiguration {

    @Bean(SecurityFilterChainBeanNames.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN)
    @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, AuthenticationConfiguration authenticationConfiguration) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // enable resource owner password credentials grant
        EulerAuthorizationServerConfiguration.configPasswordAuthentication(http, authenticationConfiguration);
        // return original user principal if client support
        // EulerAuthorizationServerConfiguration.configPrincipalSupportTokenIntrospectionAuthentication(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(withDefaults());
        http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
        http.exceptionHandling((exceptions) -> exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"), createRequestMatcher()));
        return http.build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.oauth2.authorizationserver", name = "authorization-store-type", havingValue = "jdbc")
    static class JdbcAuthorizationServerConfiguration {
        @Bean
        @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
        public OAuth2AuthorizationService jdbcOAuth2AuthorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
            return new EulerJdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        }

        @Bean
        @ConditionalOnMissingBean(OAuth2AuthorizationConsentService.class)
        public OAuth2AuthorizationConsentService jdbcOAuth2AuthorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
            return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.oauth2.authorizationserver", name = "authorization-store-type", havingValue = "redis")
    static class RedisAuthorizationServerConfiguration {

        @Bean
        @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
        public OAuth2AuthorizationService redisOAuth2AuthorizationService(
                StringRedisTemplate stringRedisTemplate,
                RegisteredClientRepository registeredClientRepository,
                EulerBootAuthorizationServerProperties eulerBootAuthorizationServerProperties) {
            EulerRedisOAuth2AuthorizationService authorizationService = new EulerRedisOAuth2AuthorizationService(
                    stringRedisTemplate,
                    registeredClientRepository,
                    eulerBootAuthorizationServerProperties.getAuthorizationLifetime());
            authorizationService.setKeyPrefix(eulerBootAuthorizationServerProperties.getRedisKeyPrefix());
            return authorizationService;
        }

        @Bean
        @ConditionalOnMissingBean(OAuth2AuthorizationConsentService.class)
        public OAuth2AuthorizationConsentService jdbcOAuth2AuthorizationConsentService(
                StringRedisTemplate stringRedisTemplate,
                RegisteredClientRepository registeredClientRepository,
                EulerBootAuthorizationServerProperties eulerBootAuthorizationServerProperties) {
            EulerRedisOAuth2AuthorizationConsentService authorizationConsentService = new EulerRedisOAuth2AuthorizationConsentService(
                    stringRedisTemplate,
                    registeredClientRepository,
                    eulerBootAuthorizationServerProperties.getAuthorizationLifetime());
            authorizationConsentService.setKeyPrefix(eulerBootAuthorizationServerProperties.getRedisKeyPrefix());
            return authorizationConsentService;
        }
    }

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        BearerTokenAuthenticationUserContext oauth2AuthenticatedPrincipalUserContext = new BearerTokenAuthenticationUserContext();
        UsernamePasswordAuthenticationUserContext usernamePasswordAuthenticationUserContext = new UsernamePasswordAuthenticationUserContext();
        return new DelegatingUserContext(oauth2AuthenticatedPrincipalUserContext, usernamePasswordAuthenticationUserContext);
    }

    private static RequestMatcher createRequestMatcher() {
        MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return requestMatcher;
    }
}
