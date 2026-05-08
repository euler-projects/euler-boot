/*
 * Copyright 2013-present the original author or authors.
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

import com.nimbusds.jose.jwk.JWK;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.EulerBootAuthorizationServerJwkProperties.KeyDefinition;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.util.JwkEntryParser;
import org.eulerframework.security.jwk.*;
import org.eulerframework.security.jwk.source.ManagedJwkSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Auto-configuration that replaces Spring Boot's built-in
 * {@link com.nimbusds.jose.jwk.source.ImmutableJWKSet} with a {@link ManagedJwkSource}
 * whenever at least one JWK is predefined through the Spring Boot configuration file.
 * Unlike the immutable default, {@link ManagedJwkSource} resolves JWKs dynamically from
 * a {@link JwkRepository}, so key changes are picked up at runtime.
 * <p>
 * This configuration also redefines the {@link NimbusJwtEncoder} bean so that multiple
 * usable JWKs sharing the same signature algorithm can coexist.
 * <p>
 * Every bean exposed here can be overridden by an application-defined bean of the same
 * type. Partial overrides are supported as well &mdash; for example, a project may only
 * customise {@link ManagedJwkSource} while still relying on the {@link NimbusJwtEncoder}
 * built by {@link EulerOAuth2AuthorizationServerJwkConfiguration#jwtEncoder(ManagedJwkSource)},
 * or only replace {@link JwkRepository} and keep everything else.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ManagedJwkSource.class)
@Conditional(OnJwkKeysConfiguredCondition.class)
public class EulerOAuth2AuthorizationServerJwkConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EulerOAuth2AuthorizationServerJwkConfiguration.class);

    /**
     * The default {@link JwtEncoder} wired by {@code OAuth2ConfigurerUtils} does not handle
     * the case where several keys share the same signature algorithm. This method replaces
     * it with a {@link NimbusJwtEncoder} that installs a custom {@code JwkSelector}: when
     * multiple JWKs match the requested algorithm the selector picks the first one that
     * carries a private key and uses it for signing.
     *
     * @param jwkSource managed source backing the encoder; must expose at least one JWK with
     *                  a private part for every signing algorithm used by the application
     * @return a {@link NimbusJwtEncoder} configured with the private-key-preferring selector
     */
    @Bean
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder(ManagedJwkSource jwkSource) {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        jwtEncoder.setJwkSelector(jwks -> jwks.stream()
                .filter(JWK::isPrivate)
                .findFirst()
                .orElseThrow(() -> new JwtEncodingException(String.format(
                        "An error occurred while attempting to encode the Jwt: " +
                                "all available key for for the signing algorithm [%s] does not contain a private key.",
                        jwks.getFirst().getAlgorithm()))));
        return jwtEncoder;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(ManagedJwkSource.class)
    static class ManagedJwkSourceConfiguration {
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        ManagedJwkSource managedJwkSource(JwkRepository repository) {
            return new ManagedJwkSource(repository);
        }

        @Bean
        @ConditionalOnMissingBean(JwkRepository.class)
        @ConditionalOnBean(JwkManageService.class)
        JwkRepository managedJwkRepository(
                JwkManageService manageService,
                EulerBootAuthorizationServerJwkProperties props,
                ResourceLoader resourceLoader) {
            List<JwkEntry> initialEntries = parseJwkEntries(props, new JwkEntryParser(resourceLoader));
            ManagedJwkRepository repository = new ManagedJwkRepository(manageService, initialEntries);
            LOGGER.info("Managed JWK repository initialized with {} preconfigured jwks", initialEntries.size());
            return repository;
        }

        @Bean
        @ConditionalOnMissingBean({
                JwkRepository.class,
                JwkManageService.class
        })
        JwkRepository inMemoryJwkRepository(EulerBootAuthorizationServerJwkProperties props,
                                            ResourceLoader resourceLoader) {
            List<JwkEntry> initialEntries = parseJwkEntries(props, new JwkEntryParser(resourceLoader));
            InMemoryJwkRepository repository = new InMemoryJwkRepository(initialEntries);
            LOGGER.info("In memory JWK repository initialized with {} preconfigured jwks", initialEntries.size());
            return repository;
        }

        private static List<JwkEntry> parseJwkEntries(
                EulerBootAuthorizationServerJwkProperties props,
                JwkEntryParser parser) {
            Map<String, KeyDefinition> keys = props.getKeys();
            List<JwkEntry> jwkEntries = new ArrayList<>(keys.size());
            for (Map.Entry<String, KeyDefinition> e : keys.entrySet()) {
                KeyDefinition def = e.getValue();
                if (!StringUtils.hasText(def.getKid())) {
                    def.setKid(e.getKey());
                }
                jwkEntries.add(parser.parse(def));
            }
            return jwkEntries;
        }
    }
}
