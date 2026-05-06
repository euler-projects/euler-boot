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
 * 当通过 Spring Boot 配置文件至少预定义了一个 JWK 的时候, 使用 {@link ManagedJwkSource}
 * 覆盖 Spring Boot 内置的 {@link com.nimbusds.jose.jwk.source.ImmutableJWKSet},
 * {@link ManagedJwkSource} 支持从 {@link JwkRepository} 中动态读取 JWK 数据.
 * <p>
 * 本配置文件还会重新定义 {@link NimbusJwtEncoder} bean, 以支持同一签名算法存在多个可用的 JWK.
 * <p>
 * 以上所有 bean 都可以被开发者在实现项目中通自行定义相同类型的 bean 来覆盖. 并且允许部分覆盖.
 * <p>
 * 例如仅自定义 {@link ManagedJwkSource}, 仍然使用
 * {@link EulerOAuth2AuthorizationServerJwkConfiguration#jwtEncoder(ManagedJwkSource)}
 * 预定义的 {@link NimbusJwtEncoder}.
 * <p>
 * 或者仅自定义 {@link JwkRepository}.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ManagedJwkSource.class)
@Conditional(OnJwkKeysConfiguredCondition.class)
public class EulerOAuth2AuthorizationServerJwkConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EulerOAuth2AuthorizationServerJwkConfiguration.class);

    /**
     * {@code OAuth2ConfigurerUtils} 配置的默认 {@link JwtEncoder} 不支持统一签名算法有多个 key 的情况,
     * 这里重新定一个 {@link NimbusJwtEncoder}, 并设置自定义的 {@code JwkSelector}, 选择策略为当存在多个相同签名算法的 key 时,
     * 直接选择包含私钥且最靠前的那个作为签名 key.
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
