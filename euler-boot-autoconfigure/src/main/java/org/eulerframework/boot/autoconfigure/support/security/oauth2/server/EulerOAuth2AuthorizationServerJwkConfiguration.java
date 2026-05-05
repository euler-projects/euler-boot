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

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.EulerBootAuthorizationServerJwkProperties.KeyDefinition;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.util.JwkEntryParser;
import org.eulerframework.security.oauth2.server.authorization.jwk.*;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.ClusteredReloadableJwkSource;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.ReloadableJwkSource;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.StandaloneReloadableJwkSource;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.coordinator.JwkClusterCoordinator;
import org.eulerframework.security.oauth2.server.authorization.jwk.source.coordinator.RedisJwkClusterCoordinator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Autoconfiguration for the JWK subsystem: repository, reloadable JWK source
 * (standalone or clustered), optional key generator, observability, and the
 * Nimbus {@link JWKSource} bean consumed by Spring Authorization Server.
 *
 * <h3>Activation</h3>
 * Gated by two class-level conditions:
 * <ol>
 *   <li>{@code @ConditionalOnClass(ReloadableJwkSource.class)} &mdash; the
 *       {@code euler-security-oauth2-authorization-server} module must be on
 *       the classpath.</li>
 *   <li>{@code @Conditional(OnJwkKeysConfiguredCondition.class)} &mdash; at
 *       least one entry must be declared under
 *       {@code euler.security.oauth2.authorizationserver.jwk.keys}. Leaving
 *       the map empty disables the entire subsystem and Spring Authorization
 *       Server falls back to its built-in ephemeral signing key.</li>
 * </ol>
 *
 * <h3>Repository wiring</h3>
 * <ul>
 *   <li>When a {@link JwkManageService} bean is present the repository is a
 *       {@link PersistentJwkRepository} that bridges to the management service.
 *       The pre-configured keys are then upserted into the persistent backend
 *       on every startup so deployments restart from a known baseline.</li>
 *   <li>When no {@link JwkManageService} is present the repository is an
 *       {@link InMemoryJwkRepository} built directly from the pre-configured
 *       entries. No admin-driven mutation endpoints are exposed.</li>
 * </ul>
 *
 * <h3>Source wiring</h3>
 * Controlled by {@code ...jwk.manager.type}:
 * <ul>
 *   <li>{@code standalone} (default) &mdash; {@link StandaloneReloadableJwkSource}.</li>
 *   <li>{@code clustered} &mdash; {@link ClusteredReloadableJwkSource} backed by a
 *       {@link JwkClusterCoordinator}. When no custom coordinator bean is
 *       supplied, the nested Redis coordinator configuration auto-wires a
 *       Redis-backed coordinator together with its
 *       {@link RedisMessageListenerContainer}.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ReloadableJwkSource.class)
@Conditional(OnJwkKeysConfiguredCondition.class)
public class EulerOAuth2AuthorizationServerJwkConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EulerOAuth2AuthorizationServerJwkConfiguration.class);

    /**
     * Persistent repository backed by an application-supplied
     * {@link JwkManageService}. The pre-configured entries are upserted on
     * every startup so the cluster converges to the declared baseline.
     */
    @Bean
    @ConditionalOnMissingBean(JwkRepository.class)
    @ConditionalOnBean(JwkManageService.class)
    JwkRepository persistentJwkRepository(JwkManageService manageService,
                                          EulerBootAuthorizationServerJwkProperties props,
                                          ResourceLoader resourceLoader) {
        PersistentJwkRepository repository = new PersistentJwkRepository(manageService);
        List<JwkEntry> bootstrap = preconfiguredEntries(props, new JwkEntryParser(resourceLoader));
        for (JwkEntry entry : bootstrap) {
            repository.save(entry);
        }
        LOGGER.info("JWK repository: PersistentJwkRepository (bootstrap upserted {} preconfigured entries)",
                bootstrap.size());
        return repository;
    }

    /**
     * In-memory repository used when no {@link JwkManageService} bean is
     * present. The repository snapshot is immutable across the lifetime of
     * the application: mutation endpoints are intentionally not wired in
     * this profile.
     */
    @Bean
    @ConditionalOnMissingBean({JwkRepository.class, JwkManageService.class})
    JwkRepository inMemoryJwkRepository(EulerBootAuthorizationServerJwkProperties props,
                                        ResourceLoader resourceLoader) {
        List<JwkEntry> entries = preconfiguredEntries(props, new JwkEntryParser(resourceLoader));
        LOGGER.info("JWK repository: InMemoryJwkRepository ({} preconfigured entries)", entries.size());
        return new InMemoryJwkRepository(entries);
    }

    // ---- reloadable sources ----

    /**
     * Standalone single-node {@link ReloadableJwkSource} for non-clustered
     * deployments and tests. Activated when
     * {@code ...jwk.manager.type=standalone} (default when absent).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.oauth2.authorizationserver.jwk.manager",
            name = "type", havingValue = "standalone", matchIfMissing = true)
    static class StandaloneReloadableJwkSourceConfiguration {

        @Bean
        JWKSource<SecurityContext> reloadableJwkSource(JwkRepository repository,
                                                       ApplicationEventPublisher publisher) {
            StandaloneReloadableJwkSource source = new StandaloneReloadableJwkSource(repository, publisher);
            source.reload();
            LOGGER.info("JWK source: StandaloneReloadableJwkSource (single-node)");
            return source;
        }
    }

    /**
     * Clustered {@link ReloadableJwkSource} for multi-node deployments.
     * Activated when {@code ...jwk.manager.type=clustered}. Requires a
     * {@link JwkClusterCoordinator} bean; the nested Redis coordinator
     * configuration supplies a default when none is provided.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.oauth2.authorizationserver.jwk.manager",
            name = "type", havingValue = "clustered")
    @EnableScheduling
    static class ClusteredReloadableJwkSourceConfiguration {

        @Bean
        JWKSource<SecurityContext> reloadableJwkSource(JwkRepository repository,
                                                       ApplicationEventPublisher publisher,
                                                       JwkClusterCoordinator coordinator,
                                                       TaskScheduler scheduler,
                                                       EulerBootAuthorizationServerJwkProperties props) {
            EulerBootAuthorizationServerJwkProperties.Manager.Cluster cfg =
                    props.getManager().getCluster();
            ClusteredReloadableJwkSource.ClusteredReloadableJwkSourceOptions options = new ClusteredReloadableJwkSource.ClusteredReloadableJwkSourceOptions(
                    cfg.getNodeId(),
                    cfg.getHeartbeatInterval(),
                    cfg.getHeartbeatTtl(),
                    cfg.getSafetyReloadInterval());
            ClusteredReloadableJwkSource source = new ClusteredReloadableJwkSource(repository, coordinator,
                    publisher, scheduler, options);
            LOGGER.info("JWK source: ClusteredReloadableJwkSource (nodeId={}, coordinator={})",
                    source.nodeId(), coordinator.getClass().getSimpleName());
            return source;
        }

        /**
         * Default Redis-backed coordinator wiring. The class-level
         * {@code @ConditionalOnMissingBean(JwkClusterCoordinator.class)}
         * ensures both the {@link RedisMessageListenerContainer} bean and
         * the {@link RedisJwkClusterCoordinator} bean are either both
         * active or both skipped.
         */
        @Configuration(proxyBeanMethods = false)
        @ConditionalOnMissingBean(JwkClusterCoordinator.class)
        static class DefaultRedisCoordinatorConfiguration {

            @Bean
            @ConditionalOnMissingBean(RedisMessageListenerContainer.class)
            RedisMessageListenerContainer redisMessageListenerContainer(
                    RedisConnectionFactory connectionFactory) {
                RedisMessageListenerContainer container = new RedisMessageListenerContainer();
                container.setConnectionFactory(connectionFactory);
                return container;
            }

            @Bean
            JwkClusterCoordinator jwkClusterCoordinator(
                    StringRedisTemplate redis,
                    RedisMessageListenerContainer listeners,
                    EulerBootAuthorizationServerJwkProperties props) {
                RedisJwkClusterCoordinator.RedisJwkClusterCoordinatorOptions options = new RedisJwkClusterCoordinator.RedisJwkClusterCoordinatorOptions(
                        props.getManager().getCluster().getRedis().getNamespace());
                return new RedisJwkClusterCoordinator(redis, listeners, options);
            }
        }
    }

    // ---- helpers ----

    /**
     * Materialize the {@code euler.security.oauth2.authorizationserver.jwk.keys}
     * map into {@link JwkEntry} instances. The map key is used as the
     * fallback {@code kid} when a {@link KeyDefinition} does not declare an
     * explicit one, so end-users can keep YAML compact in the common case
     * (one entry, kid == map key) yet override the {@code kid} when needed.
     */
    private static List<JwkEntry> preconfiguredEntries(EulerBootAuthorizationServerJwkProperties props,
                                                       JwkEntryParser parser) {
        Map<String, KeyDefinition> keys = props.getKeys();
        List<JwkEntry> out = new ArrayList<>(keys.size());
        for (Map.Entry<String, KeyDefinition> e : keys.entrySet()) {
            KeyDefinition def = e.getValue();
            if (def.getKid() == null || def.getKid().isBlank()) {
                def.setKid(e.getKey());
            }
            out.add(parser.parse(def));
        }
        return List.copyOf(out);
    }
}
