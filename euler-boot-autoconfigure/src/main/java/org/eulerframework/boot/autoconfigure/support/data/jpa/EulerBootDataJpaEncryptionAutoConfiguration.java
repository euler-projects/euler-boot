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

import jakarta.persistence.EntityManager;
import org.eulerframework.security.crypto.AesGcmDataCipher;
import org.eulerframework.security.crypto.DataCipher;
import org.eulerframework.security.crypto.DelegatingDataCipher;
import org.eulerframework.security.crypto.InMemoryKeyRepository;
import org.eulerframework.security.crypto.KeyMaterialLoader;
import org.eulerframework.security.crypto.KeyRepository;
import org.eulerframework.security.crypto.NoopDataCipher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires the JPA-layer data-encryption stack from
 * {@link EulerBootDataJpaEncryptionProperties}.
 *
 * <p>The {@link DataCipher} bean produced here is the single injection point
 * used by every {@code AbstractEncryptedAttributeConverter} in the
 * application; rotating keys, switching algorithms or disabling encryption
 * is therefore a configuration-only change.
 *
 * <p>Bean layout:
 * <ul>
 *   <li>{@link KeyRepository} — {@link InMemoryKeyRepository} built from
 *       {@link EulerBootDataJpaEncryptionProperties#getKeys()}.</li>
 *   <li>{@link NoopDataCipher} — always registered; can be selected as
 *       {@code primary-alg} to disable encryption for new writes while still
 *       decrypting historical ciphertexts written under other algorithms.</li>
 *   <li>{@link AesGcmDataCipher} — registered whenever the {@code AES-256-GCM}
 *       block is present in configuration.</li>
 *   <li>{@link DelegatingDataCipher} (exposed as {@link DataCipher}) — the
 *       bean consumed by every encrypted attribute converter. Its primary
 *       cipher follows
 *       {@link EulerBootDataJpaEncryptionProperties#getPrimaryAlg()}.</li>
 * </ul>
 *
 * <p>The whole auto-configuration is skipped unless at least one property
 * under the {@code euler.data.jpa.encryption} prefix is present. This means
 * that a partial configuration (e.g. {@code keys} without {@code primary-alg})
 * will still activate the auto-configuration and produce a clear fail-fast
 * error at startup rather than silently remaining inactive.
 */
@AutoConfiguration
@ConditionalOnClass({DataCipher.class, EntityManager.class})
@Conditional(EulerBootDataJpaEncryptionAutoConfiguration.OnEncryptionPropertiesPresent.class)
@EnableConfigurationProperties(EulerBootDataJpaEncryptionProperties.class)
public class EulerBootDataJpaEncryptionAutoConfiguration {

    private static final String PROPERTY_PREFIX = "euler.data.jpa.encryption";

    @Bean
    @ConditionalOnMissingBean
    public KeyRepository dataJpaEncryptionKeyRepository(EulerBootDataJpaEncryptionProperties properties) {
        InMemoryKeyRepository.Builder builder = InMemoryKeyRepository.builder();
        Map<String, EulerBootDataJpaEncryptionProperties.AlgorithmKeys> keys = properties.getKeys();
        for (Map.Entry<String, EulerBootDataJpaEncryptionProperties.AlgorithmKeys> algEntry : keys.entrySet()) {
            String alg = algEntry.getKey();
            EulerBootDataJpaEncryptionProperties.AlgorithmKeys algKeys = algEntry.getValue();
            if (algKeys == null) {
                throw new IllegalStateException(
                        PROPERTY_PREFIX + ".keys." + alg + " is present but empty");
            }
            if (!StringUtils.hasText(algKeys.getPrimaryKid())) {
                throw new IllegalStateException(
                        PROPERTY_PREFIX + ".keys." + alg + ".primary-kid is required");
            }
            if (algKeys.getItems().isEmpty()) {
                throw new IllegalStateException(
                        PROPERTY_PREFIX + ".keys." + alg + ".items must contain at least one entry");
            }
            builder.primaryKid(alg, algKeys.getPrimaryKid());
            for (Map.Entry<String, EulerBootDataJpaEncryptionProperties.KeyItem> kidEntry
                    : algKeys.getItems().entrySet()) {
                String kid = kidEntry.getKey();
                EulerBootDataJpaEncryptionProperties.KeyItem item = kidEntry.getValue();
                if (item == null) {
                    throw new IllegalStateException(
                            PROPERTY_PREFIX + ".keys." + alg + ".items." + kid + " is empty");
                }
                byte[] material = KeyMaterialLoader.load(alg, kid,
                        item.getKeyFile(), item.getPassphrase(), item.getSaltNamespace());
                builder.addKey(alg, kid, material);
            }
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataCipher dataJpaEncryptionDataCipher(EulerBootDataJpaEncryptionProperties properties,
                                                  KeyRepository keyRepository) {
        String primaryAlg = properties.getPrimaryAlg();
        if (!StringUtils.hasText(primaryAlg)) {
            throw new IllegalStateException(PROPERTY_PREFIX + ".primary-alg is required");
        }
        Map<String, DataCipher> ciphers = new LinkedHashMap<>();
        ciphers.put(NoopDataCipher.ALGORITHM, new NoopDataCipher());
        if (keyRepository.supports(AesGcmDataCipher.ALGORITHM)) {
            ciphers.put(AesGcmDataCipher.ALGORITHM, new AesGcmDataCipher(keyRepository));
        }
        return new DelegatingDataCipher(primaryAlg, ciphers);
    }

    /**
     * Matches when at least one property is bound under the
     * {@code euler.data.jpa.encryption} prefix — regardless of which specific
     * sub-key it is. This ensures the auto-configuration activates on any
     * partial configuration so that validation can produce a clear error.
     */
    static class OnEncryptionPropertiesPresent extends SpringBootCondition {

        @Override
        public ConditionOutcome getMatchOutcome(ConditionContext context,
                                                AnnotatedTypeMetadata metadata) {
            ConditionMessage.Builder message = ConditionMessage.forCondition(
                    "EulerDataJpaEncryption");
            boolean bound = Binder.get(context.getEnvironment())
                    .bind(PROPERTY_PREFIX, Bindable.mapOf(String.class, Object.class))
                    .isBound();
            if (bound) {
                return ConditionOutcome.match(
                        message.foundExactly("properties under '" + PROPERTY_PREFIX + "'"));
            }
            return ConditionOutcome.noMatch(
                    message.didNotFind("any property under '" + PROPERTY_PREFIX + "'").atAll());
        }
    }
}
