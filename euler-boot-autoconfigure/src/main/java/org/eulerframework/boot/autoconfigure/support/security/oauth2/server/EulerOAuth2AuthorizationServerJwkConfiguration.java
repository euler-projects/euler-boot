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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;


/**
 * Auto-configuration that replaces Spring Boot's built-in
 * {@link com.nimbusds.jose.jwk.source.ImmutableJWKSet} with a {@link ManagedJwkSource}
 * whenever at least one JWK is predefined through the Spring Boot configuration file.
 * Unlike the immutable default, {@link ManagedJwkSource} resolves JWKs dynamically from
 * a {@link JwkRepository}, so key changes are picked up at runtime.
 * <p>
 * This configuration also redefines the {@link NimbusJwtEncoder} bean so that it is
 * wired against the signing-only projection exposed by
 * {@link ManagedJwkSource#signingJwkSource()}, which restricts signing candidates to
 * {@link JwkStatus#ACTIVE} entries holding a private key, and installs a deterministic
 * {@code JwkSelector} tie-breaker for the rare case where several candidates share the
 * same algorithm.
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
     * Replace the default {@link JwtEncoder} wired by {@code OAuth2ConfigurerUtils}.
     * The default encoder binds against the full {@link ManagedJwkSource}, which also
     * serves the {@code /oauth2/jwks} endpoint and therefore intentionally publishes
     * {@link JwkStatus#PENDING}, {@link JwkStatus#DEPRECATED} and
     * {@link JwkStatus#VERIFY_ONLY} keys for verifier warm-up. Feeding that same set
     * into the encoder risks picking a non-ACTIVE key for signing.
     *
     * <p>Instead, this encoder is wired against
     * {@link ManagedJwkSource#signingJwkSource()}, a projection restricted to
     * {@linkplain JwkEntry#isActive() active} entries carrying a private key. Bundled
     * repositories enforce "at most one ACTIVE key per algorithm", which keeps the
     * projection unambiguous per algorithm, but a custom {@link JwkManageService} may
     * not carry that guarantee. To stay deterministic even in that case, a
     * {@code JwkSelector} tie-breaker is installed via
     * {@link NimbusJwtEncoder#setJwkSelector(org.springframework.core.convert.converter.Converter)}:
     * among candidates returned by Nimbus for a given algorithm, the entry with the
     * newer {@code iat} wins; on identical {@code iat}, the entry with the smaller
     * {@code kid} (lexicographic) wins. When no ACTIVE signing key is available, the
     * encoder fails fast rather than silently falling back onto a PENDING one.
     *
     * @param jwkSource managed source backing the encoder; must expose at least one
     *                  ACTIVE JWK with a private part for every signing algorithm used
     *                  by the application
     * @return a {@link NimbusJwtEncoder} bound to the signing-only projection, with a
     * deterministic tie-breaker installed
     */
    @Bean
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder(ManagedJwkSource jwkSource) {
        NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource.signingJwkSource());
        jwtEncoder.setJwkSelector(jwks -> jwks.stream()
                .filter(JWK::isPrivate)
                // Deterministic tie-breaker when several ACTIVE signing candidates
                // collide on the same algorithm (bundled repositories enforce
                // uniqueness, but custom JwkManageService implementations may not):
                // newer iat wins; on identical iat, smaller kid (lexicographic) wins.
                .min(Comparator
                        .comparing(JWK::getIssueTime, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JWK::getKeyID, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseThrow(() -> new JwtEncodingException(String.format(
                        "An error occurred while attempting to encode the Jwt: " +
                                "no ACTIVE signing key with a private key is available for algorithm [%s].",
                        jwks.isEmpty() ? "<none>" : jwks.getFirst().getAlgorithm()))));
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
            List<JwkEntry> initialEntries = parseJwkEntries(props.getKeys().values(), new JwkEntryParser(resourceLoader));
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
            List<JwkEntry> initialEntries = parseJwkEntries(props.getKeys().values(), new JwkEntryParser(resourceLoader));
            InMemoryJwkRepository repository = new InMemoryJwkRepository(initialEntries);
            LOGGER.info("In memory JWK repository initialized with {} preconfigured jwks", initialEntries.size());
            return repository;
        }

        private static List<JwkEntry> parseJwkEntries(
                Collection<KeyDefinition> keyDefinitions,
                JwkEntryParser parser) {
            return keyDefinitions.stream()
                    .map(parser::parse)
                    .toList();
        }
    }
}
