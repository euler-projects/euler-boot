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

import org.eulerframework.security.oauth2.server.authorization.jwk.JwkStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWK configuration properties, split into two concerns:
 * <ul>
 *   <li>{@link #getKeys() keys}: a map of {@code kid -> KeyDefinition} entries
 *       bootstrapped into the JWK subsystem at startup. With no
 *       {@code JwkManageService} bean present the map backs an in-memory
 *       repository; with a management bean present the map is upserted into
 *       the persistent backend so every deployment restarts from a known
 *       baseline.</li>
 *   <li>{@link Manager}: deployment mode and runtime parameters for the managed
 *       JWK source. Choose between {@link Manager.Type#STANDALONE} (single-node)
 *       and {@link Manager.Type#CLUSTERED} (coordinator-based multi-node).</li>
 * </ul>
 * <p>
 * Extracted from {@code EulerBootAuthorizationServerProperties} to keep the JWK
 * surface flat and discoverable; the property path {@code
 * euler.security.oauth2.authorizationserver.jwk.*} is preserved for end users.
 */
@ConfigurationProperties(prefix = "euler.security.oauth2.authorizationserver.jwk")
public class EulerBootAuthorizationServerJwkProperties {

    /**
     * Pre-configured keys keyed by their effective {@code kid}. Iteration
     * preserves declaration order (Spring Boot binds maps as
     * {@link LinkedHashMap}) so deterministic bootstrap upsert is possible.
     * The map key is used as the {@code kid} of each entry by default; a
     * {@link KeyDefinition#getKid() kid} explicitly declared on the entry
     * itself takes precedence. Leaving the map empty disables the Euler JWK
     * subsystem entirely: Spring Authorization Server falls back to its
     * built-in {@code ImmutableJWKSet} with a randomly-generated keypair.
     */
    private Map<String, KeyDefinition> keys = new LinkedHashMap<>();

    private Manager manager = new Manager();

    public Map<String, KeyDefinition> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, KeyDefinition> keys) {
        this.keys = keys;
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(Manager manager) {
        this.manager = manager;
    }

    /**
     * Declarative definition of a single JWK entry. Carries the algorithm,
     * use, lifecycle status, issued-at timestamp, PEM location and an
     * optional explicit {@code kid}. When {@link #getKid()} is left blank
     * the autoconfiguration falls back to the enclosing
     * {@link #getKeys() keys} map key.
     */
    public static class KeyDefinition {
        /**
         * Optional explicit JWK {@code kid}. When {@code null} or blank the
         * autoconfiguration falls back to the enclosing
         * {@link #getKeys() keys} map key, so most deployments can omit this
         * field. Set it explicitly only when the desired {@code kid} differs
         * from the YAML key (for example when migrating between two map
         * layouts while preserving the JWS {@code kid} header value).
         */
        private String kid;

        /**
         * JWA algorithm: {@code RS256/RS384/RS512}, {@code ES256/ES384/ES512}.
         */
        private JwkPemAlgorithm alg = JwkPemAlgorithm.RS256;

        /**
         * JWK {@code use} parameter; defaults to {@code sig}.
         */
        private String use = "sig";

        /**
         * Lifecycle status: {@code PENDING / ACTIVE / DEPRECATED / VERIFY_ONLY / RETIRED}.
         */
        private JwkStatus status = JwkStatus.ACTIVE;

        /**
         * Issued-at timestamp rendered into the JWK's {@code iat} custom parameter.
         * Parsed by Spring Boot as ISO-8601 (e.g. {@code 2026-01-01T00:00:00Z}).
         */
        private Instant iat = Instant.now();

        /**
         * Spring {@code Resource} URL for the PEM file (supports
         * {@code file:...} / {@code classpath:...} prefixes). The file MUST
         * contain a PKCS#8 {@code -----BEGIN PRIVATE KEY-----} envelope or an
         * X.509 {@code -----BEGIN PUBLIC KEY-----} envelope; legacy PKCS#1
         * ({@code -----BEGIN RSA PRIVATE KEY-----}) is not supported.
         */
        private String pem;

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public JwkPemAlgorithm getAlg() {
            return alg;
        }

        public void setAlg(JwkPemAlgorithm alg) {
            Assert.notNull(alg, "'alg' must be specified");
            this.alg = alg;
        }

        public String getUse() {
            return use;
        }

        public void setUse(String use) {
            Assert.hasText(use, "'use' must be specified");
            this.use = use;
        }

        public JwkStatus getStatus() {
            return status;
        }

        public void setStatus(JwkStatus status) {
            Assert.notNull(status, "'status' must be specified");
            this.status = status;
        }

        public Instant getIat() {
            return iat;
        }

        public void setIat(Instant iat) {
            Assert.notNull(iat, "'iat' must be specified");
            this.iat = iat;
        }

        public String getPem() {
            return pem;
        }

        public void setPem(String pem) {
            Assert.hasText(pem, "'pem' must be specified");
            this.pem = pem;
        }
    }

    /**
     * Runtime parameters for the managed JWK source.
     */
    public static class Manager {
        /**
         * Deployment mode for the managed JWK source. Defaults to {@link Type#STANDALONE}.
         */
        private Type type = Type.STANDALONE;
        /**
         * Cluster-mode parameters; effective only when {@link #type} is {@link Type#CLUSTERED}.
         */
        private Cluster cluster = new Cluster();

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        public Cluster getCluster() {
            return cluster;
        }

        public void setCluster(Cluster cluster) {
            this.cluster = cluster;
        }

        /**
         * Deployment mode for the managed JWK source.
         * <ul>
         *   <li>{@link #STANDALONE} &mdash; single-node in-memory manager, no coordinator required.</li>
         *   <li>{@link #CLUSTERED} &mdash; multi-node manager backed by a {@code JwkClusterCoordinator}.
         *       The default coordinator implementation is Redis-backed; applications may supply a
         *       custom {@code JwkClusterCoordinator} bean to override the default.</li>
         * </ul>
         */
        public enum Type {
            STANDALONE, CLUSTERED
        }

        /**
         * Coordinator-backend-agnostic options for clustered mode. Effective only when
         * {@link Manager#getType()} is {@link Type#CLUSTERED}.
         */
        public static class Cluster {
            /**
             * Stable node id; leave blank to auto-generate as {@code hostname-pid-rand6}.
             */
            private String nodeId;
            /**
             * Heartbeat interval for refreshing the node registration.
             */
            private Duration heartbeatInterval = Duration.ofSeconds(15);
            /**
             * Heartbeat TTL; must exceed {@code heartbeatInterval}.
             */
            private Duration heartbeatTtl = Duration.ofSeconds(60);
            /**
             * Fallback reload cadence in case coordinator-driven notifications are missed.
             */
            private Duration safetyReloadInterval = Duration.ofMinutes(5);
            /**
             * Redis-backed coordinator options (effective when using the default coordinator).
             */
            private Redis redis = new Redis();

            public String getNodeId() {
                return nodeId;
            }

            public void setNodeId(String nodeId) {
                this.nodeId = nodeId;
            }

            public Duration getHeartbeatInterval() {
                return heartbeatInterval;
            }

            public void setHeartbeatInterval(Duration heartbeatInterval) {
                this.heartbeatInterval = heartbeatInterval;
            }

            public Duration getHeartbeatTtl() {
                return heartbeatTtl;
            }

            public void setHeartbeatTtl(Duration heartbeatTtl) {
                this.heartbeatTtl = heartbeatTtl;
            }

            public Duration getSafetyReloadInterval() {
                return safetyReloadInterval;
            }

            public void setSafetyReloadInterval(Duration safetyReloadInterval) {
                this.safetyReloadInterval = safetyReloadInterval;
            }

            public Redis getRedis() {
                return redis;
            }

            public void setRedis(Redis redis) {
                this.redis = redis;
            }

            /**
             * Redis-coordinator specific options.
             */
            public static class Redis {
                /**
                 * Redis key prefix shared by all cluster members; override per issuer/tenant.
                 */
                private String namespace = "euler:oauth2:jwk";

                public String getNamespace() {
                    return namespace;
                }

                public void setNamespace(String namespace) {
                    this.namespace = namespace;
                }
            }
        }
    }
}
