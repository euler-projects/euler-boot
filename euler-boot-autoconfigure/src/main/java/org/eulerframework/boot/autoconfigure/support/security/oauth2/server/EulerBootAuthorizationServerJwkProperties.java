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

import org.eulerframework.security.jwk.JwkStatus;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JWK configuration properties, split into two concerns:
 * <ul>
 *   <li>{@link #getKeys() keys}: a map of {@code id -> KeyDefinition} entries
 *       bootstrapped into the JWK subsystem at startup. The map key is an
 *       opaque logical id used solely as a stable coordinate so that different
 *       Spring profiles (e.g. {@code application-prod.yml},
 *       {@code application-staging.yml}) can override the same entry; it has
 *       no bearing on the JWK {@code kid} header, which MUST be declared
 *       explicitly on {@link KeyDefinition#getKid()}. With no
 *       {@code JwkManageService} bean present the map backs an in-memory
 *       repository; with a management bean present the map is upserted into
 *       the persistent backend so every deployment restarts from a known
 *       baseline.</li>
 * </ul>
 * <p>
 * Extracted from {@code EulerBootAuthorizationServerProperties} to keep the JWK
 * surface flat and discoverable; the property path {@code
 * euler.security.oauth2.authorizationserver.jwk.*} is preserved for end users.
 */
@ConfigurationProperties(prefix = "euler.security.oauth2.authorizationserver.jwk")
public class EulerBootAuthorizationServerJwkProperties {

    /**
     * Pre-configured keys keyed by an opaque logical id. Iteration preserves
     * declaration order (Spring Boot binds maps as {@link LinkedHashMap}) so
     * deterministic bootstrap upsert is possible.
     * <p>
     * The map key is purely a profile-override coordinate &mdash; different
     * {@code application-*.yml} files may redefine the same logical id to
     * publish a different {@code kid} per deployment environment, thereby
     * avoiding cross-environment {@code kid} collisions. It is NOT used as
     * the JWK {@code kid}; every {@link KeyDefinition} MUST carry an explicit
     * non-blank {@link KeyDefinition#getKid() kid}.
     * <p>
     * Leaving the map empty disables the Euler JWK subsystem entirely: Spring
     * Authorization Server falls back to its built-in {@code ImmutableJWKSet}
     * with a randomly-generated keypair.
     */
    private Map<String, KeyDefinition> keys = new LinkedHashMap<>();

    public Map<String, KeyDefinition> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, KeyDefinition> keys) {
        this.keys = keys;
    }

    /**
     * Declarative definition of a single JWK entry. Carries the {@code kid},
     * algorithm, use, lifecycle status, issued-at timestamp and PEM key file
     * location. The {@link #getKid() kid} is mandatory and is the sole source
     * of the JWK {@code kid} header &mdash; it is never defaulted from the
     * enclosing {@link #getKeys() keys} map key, which is reserved for
     * profile-based overriding.
     */
    public static class KeyDefinition {
        /**
         * Mandatory JWK {@code kid}. Must be non-blank; startup fails fast
         * otherwise. The enclosing {@link #getKeys() keys} map key is an
         * opaque profile-override coordinate and is NOT used as a fallback.
         * Declaring {@code kid} explicitly lets different Spring profiles
         * publish distinct {@code kid} values for the same logical entry
         * across deployment environments without collisions.
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
         * Spring {@code Resource} URL for the PEM key file (supports
         * {@code file:...} / {@code classpath:...} prefixes). The file MUST
         * contain a PKCS#8 {@code -----BEGIN PRIVATE KEY-----} envelope or an
         * X.509 {@code -----BEGIN PUBLIC KEY-----} envelope; legacy PKCS#1
         * ({@code -----BEGIN RSA PRIVATE KEY-----}) is not supported.
         *
         * <p>Generate key files with:
         * <pre>{@code
         * # RSA 2048 (RS256)
         * openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out rs256.pem
         *
         * # RSA 3072 (RS384)
         * openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out rs384.pem
         *
         * # RSA 4096 (RS512)
         * openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:4096 -out rs512.pem
         *
         * # EC P-256 (ES256)
         * openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out es256.pem
         *
         * # EC P-384 (ES384)
         * openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-384 -out es384.pem
         *
         * # EC P-521 (ES512)
         * openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-521 -out es512.pem
         * }</pre>
         */
        private String keyFile;

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            Assert.hasText(kid, "'kid' must be specified");
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

        public String getKeyFile() {
            return keyFile;
        }

        public void setKeyFile(String keyFile) {
            Assert.hasText(keyFile, "'key-file' must be specified");
            this.keyFile = keyFile;
        }
    }
}
