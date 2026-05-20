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
package org.eulerframework.boot.autoconfigure.support.data.jpa;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration binding for the JPA-layer data-encryption stack.
 *
 * <p>These properties drive the {@code DataCipher} bean that every
 * {@code AbstractEncryptedAttributeConverter} in the application consumes.
 * They are deliberately located at the JPA layer rather than underneath any
 * domain-specific prefix, because a single {@code DataCipher} instance
 * governs the encryption of <em>all</em> encrypted entity columns.
 *
 * <p>Binds the following shape under prefix {@code euler.data.jpa.encryption}:
 *
 * <pre>
 * euler:
 *   data:
 *     jpa:
 *       encryption:
 *         primary-alg: AES-256-GCM      # or "noop"
 *         keys:
 *           my-key:                      # opaque logical id (profile-override coordinate)
 *             kid: k1                    # actual key identifier
 *             alg: AES-256-GCM          # algorithm this key belongs to
 *             primary: true              # marks this as the primary kid for its alg
 *             key-file: /abs/path        # key file path (format depends on algorithm)
 *             properties:                # algorithm-specific properties
 *               passphrase: dev          # fallback: derive key via SHA-256 (dev only)
 * </pre>
 *
 * <p>Each distinct {@code alg} among the keys MUST have exactly one entry
 * with {@code primary: true}. The {@code primary-alg} selects which
 * algorithm is used for new writes; {@code noop} is always available and
 * needs no entry under {@code keys}.
 *
 * @see EulerBootDataJpaEncryptionAutoConfiguration
 */
@ConfigurationProperties(prefix = "euler.data.jpa.encryption")
public class EulerBootDataJpaEncryptionProperties {

    /**
     * Algorithm identifier used for writes. MUST be either {@code "noop"}
     * or an {@code alg} present among the {@link #keys} entries. Matched
     * case-insensitively.
     */
    private String primaryAlg;

    /**
     * Flat map of encryption key definitions keyed by an opaque logical id.
     * The map key is purely a profile-override coordinate (so that different
     * {@code application-*.yml} files can override the same logical entry);
     * it is NOT used as the key's {@code kid}. Every {@link KeyDefinition}
     * MUST carry an explicit non-blank {@link KeyDefinition#getKid() kid}.
     * <p>
     * Iteration preserves declaration order ({@link LinkedHashMap}).
     */
    private Map<String, KeyDefinition> keys = new LinkedHashMap<>();

    public String getPrimaryAlg() {
        return primaryAlg;
    }

    public void setPrimaryAlg(String primaryAlg) {
        this.primaryAlg = primaryAlg;
    }

    public Map<String, KeyDefinition> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, KeyDefinition> keys) {
        this.keys = keys;
    }

    /**
     * Definition of a single encryption key. Each entry declares the key
     * identity, file-based key source, and algorithm-specific parameters
     * via a generic {@link #properties} map.
     *
     * <p>The common fields ({@link #kid}, {@link #alg}, {@link #primary},
     * {@link #keyFile}) are shared across all algorithms. Algorithm-specific
     * parameters (e.g. {@code passphrase}) live inside {@link #properties}
     * and are validated at key-material load time.
     */
    public static class KeyDefinition {

        /**
         * Mandatory key identifier. Stamped into the encrypted envelope
         * header so that the correct key can be located during decryption.
         * Must be non-blank and unique within the same {@link #alg}.
         */
        private String kid;

        /**
         * Algorithm this key belongs to (e.g. {@code AES-256-GCM}).
         * Must be non-blank.
         */
        private String alg;

        /**
         * Whether this key is the primary for its {@link #alg}. Exactly
         * one key per algorithm MUST be marked {@code primary: true}; it
         * is the key used to encrypt new data under that algorithm.
         */
        private boolean primary;

        /**
         * Path or Spring Resource URL for the key file. The expected file
         * format depends on the algorithm (e.g. 32-byte raw binary for
         * {@code AES-256-GCM}). Takes precedence over any key material
         * source in {@link #properties}.
         *
         * <p>Generate a 256-bit random key file with:
         * <pre>{@code
         * openssl rand 32 > /path/to/aes256.key
         * }</pre>
         */
        private String keyFile;

        /**
         * Algorithm-specific properties used to resolve key material when
         * {@link #keyFile} is not provided. For {@code AES-256-GCM}:
         * <ul>
         *   <li>{@code passphrase} — development-only passphrase; key is
         *       derived via SHA-256.</li>
         * </ul>
         * Missing required properties result in a fail-fast startup error.
         */
        private Map<String, String> properties = Collections.emptyMap();

        public String getKid() {
            return kid;
        }

        public void setKid(String kid) {
            this.kid = kid;
        }

        public String getAlg() {
            return alg;
        }

        public void setAlg(String alg) {
            this.alg = alg;
        }

        public boolean isPrimary() {
            return primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public String getKeyFile() {
            return keyFile;
        }

        public void setKeyFile(String keyFile) {
            this.keyFile = keyFile;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }
}
