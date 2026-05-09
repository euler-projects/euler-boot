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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration binding for the JPA-layer data-encryption stack.
 *
 * <p>These properties drive the {@code DataCipher} bean that every
 * {@code AbstractEncryptedAttributeConverter} in the application consumes.
 * They are deliberately located at the JPA layer rather than underneath any
 * domain-specific prefix, because a single {@code DataCipher} instance
 * governs the encryption of <em>all</em> encrypted entity columns — not just
 * JWK material.
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
 *           AES-256-GCM:
 *             primary-kid: k1
 *             items:
 *               k1:
 *                 key-file: /abs/path   # 32-byte POSIX-0600 file
 *                 passphrase: ...        # fallback when key-file is blank
 * </pre>
 *
 * <p>{@code noop} is always registered by
 * {@link EulerBootDataJpaEncryptionAutoConfiguration} and needs no entry under
 * {@code keys}; it can still be named as {@code primary-alg} to disable data
 * encryption entirely (useful for development / historical-data compatibility).
 */
@ConfigurationProperties(prefix = "euler.data.jpa.encryption")
public class EulerBootDataJpaEncryptionProperties {

    /**
     * Algorithm identifier used for writes. MUST be either {@code "noop"}
     * or a key present in {@link #keys}. Matched case-insensitively.
     */
    private String primaryAlg;

    /**
     * Registered keyed algorithms. Key is the algorithm identifier
     * (e.g. {@code AES-256-GCM}); the value carries the primary {@code
     * kid} plus the per-{@code kid} material source. Algorithms without
     * keys (i.e. {@code noop}) do not appear here.
     */
    private final Map<String, AlgorithmKeys> keys = new LinkedHashMap<>();

    public String getPrimaryAlg() {
        return primaryAlg;
    }

    public void setPrimaryAlg(String primaryAlg) {
        this.primaryAlg = primaryAlg;
    }

    public Map<String, AlgorithmKeys> getKeys() {
        return keys;
    }

    /** Per-algorithm key-rotation block. */
    public static class AlgorithmKeys {

        /**
         * Identifier of the {@code kid} that new ciphertexts are encrypted
         * under. MUST be a key in {@link #items}.
         */
        private String primaryKid;

        /** Per-{@code kid} material source. */
        private final Map<String, KeyItem> items = new LinkedHashMap<>();

        public String getPrimaryKid() {
            return primaryKid;
        }

        public void setPrimaryKid(String primaryKid) {
            this.primaryKid = primaryKid;
        }

        public Map<String, KeyItem> getItems() {
            return items;
        }
    }

    /**
     * Single key material source. {@link #keyFile} takes precedence; when it
     * is blank, falls back to {@link #passphrase} (development only). Both
     * blank is a fail-fast at startup.
     */
    public static class KeyItem {

        /**
         * Absolute file system path to a 32-byte binary KEY. POSIX permissions
         * MUST be {@code 0600} (owner read/write only).
         */
        private String keyFile;

        /**
         * Development-only passphrase used to derive 32 bytes via
         * PBKDF2-HMAC-SHA256 (600k iterations, salt derived from
         * {@link #saltNamespace} + the {@code kid}). Consulted only when
         * {@link #keyFile} is blank.
         */
        private String passphrase;

        /**
         * Optional override for the PBKDF2 salt namespace, only used on the
         * passphrase path. Blank/unset falls back to the framework default
         * ({@code "euler-data-key/"}). Set this to the historical value of a
         * pre-existing deployment (e.g. {@code "euler-uc-data-key/"}) to keep
         * legacy passphrase-derived ciphertexts decryptable.
         */
        private String saltNamespace;

        public String getKeyFile() {
            return keyFile;
        }

        public void setKeyFile(String keyFile) {
            this.keyFile = keyFile;
        }

        public String getPassphrase() {
            return passphrase;
        }

        public void setPassphrase(String passphrase) {
            this.passphrase = passphrase;
        }

        public String getSaltNamespace() {
            return saltNamespace;
        }

        public void setSaltNamespace(String saltNamespace) {
            this.saltNamespace = saltNamespace;
        }
    }
}
