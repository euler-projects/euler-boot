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
package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.boot.autoconfigure.support.data.jpa.EulerDataJpaAuditingAutoConfiguration;
import org.eulerframework.security.authentication.ChallengeService;
import org.eulerframework.security.authentication.InMemoryChallengeService;
import org.eulerframework.security.authentication.appattest.apple.AppleAppAttestValidationService;
import org.eulerframework.security.authentication.appattest.apple.DefaultAppleAppAttestValidationService;
import org.eulerframework.security.authentication.appattest.*;
import org.eulerframework.security.authentication.otp.InMemoryOneTimePasswordService;
import org.eulerframework.security.authentication.otp.JdbcOneTimePasswordService;
import org.eulerframework.security.authentication.otp.OneTimePasswordChannel;
import org.eulerframework.security.authentication.otp.OneTimePasswordGenerator;
import org.eulerframework.security.authentication.otp.OneTimePasswordPolicyResolver;
import org.eulerframework.security.authentication.otp.OneTimePasswordService;
import org.eulerframework.security.authentication.otp.RedisOneTimePasswordService;
import org.eulerframework.security.authentication.otp.SecureRandomOneTimePasswordGenerator;
import org.eulerframework.security.authentication.otp.StaticOneTimePasswordPolicyResolver;
import org.eulerframework.security.authentication.otp.StdoutOneTimePasswordChannel;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.context.UserDetailsPrincipalUserContext;
import org.eulerframework.security.provisioning.jit.JitProvisioningPolicyResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

import java.util.List;

@AutoConfiguration(
        before = {
                EulerDataJpaAuditingAutoConfiguration.class
        },
        beforeName = {
                "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
                "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
        })
@EnableConfigurationProperties({
        EulerSecurityProperties.class,
        EulerSecurityAuthenticationAppAttestProperties.class,
        EulerSecurityAuthenticationOneTimePasswordProperties.class,
        EulerSecurityAuthenticationWechatProperties.class
})
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
public class EulerSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        return new UserDetailsPrincipalUserContext();
    }

    /**
     * Resolves the JIT provisioning policy for an identity type from
     * the {@code euler.security.identity-type} declarations.
     * Undeclared identity types are provisioned with the defaults
     * (enabled, authorities {@code [user]}).
     */
    @Bean
    @ConditionalOnMissingBean(JitProvisioningPolicyResolver.class)
    public JitProvisioningPolicyResolver jitProvisioningPolicyResolver(
            EulerSecurityProperties securityProperties) {
        return JitProvisioningPropertiesMapper.asResolver(securityProperties.getIdentityType());
    }

    @Bean
    @ConditionalOnProperty(prefix = "euler.security.authentication.wechat", name = "enabled")
    static public InitializeWechatUserDetailsBeanManagerConfigurer initializeWechatLoginBeanManagerConfigurer(ApplicationContext context) {
        return new InitializeWechatUserDetailsBeanManagerConfigurer(context);
    }

    @Bean
    @ConditionalOnProperty(prefix = "euler.security.authentication.otp", name = "enabled", havingValue = "true")
    static public InitializeOneTimePasswordAuthenticationProviderManagerConfigurer initializeOneTimePasswordLoginBeanManagerConfigurer(ApplicationContext context) {
        return new InitializeOneTimePasswordAuthenticationProviderManagerConfigurer(context);
    }

    /**
     * Autoconfiguration for App Attest related beans.
     * <p>
     * Provides default implementations of {@link RegisteredAppRepository},
     * {@link AppAttestAttestationRegistrationService}, {@link ChallengeService} and
     * {@link AppleAppAttestValidationService}. The default validation service is
     * {@link DefaultAppleAppAttestValidationService}, which embeds Apple's App Attestation
     * Root CA and performs full validation without any webauthn4j dependency. If an
     * application wants to use the webauthn4j-based implementation
     * ({@code Webauthn4jAppleAppAttestValidationService}), it can define its own
     * {@link AppleAppAttestValidationService} bean and this default will step aside via
     * {@link ConditionalOnMissingBean}.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.authentication.app-attest", name = "enabled", havingValue = "true")
    static class DeviceAttestBeanConfiguration {

        /**
         * Service-backed {@link RegisteredAppRepository} used when an
         * {@link AppAttestAppService} bean is present in the context. This bean is a bare
         * bridge. Apps declared under {@code euler.security.authentication.app-attest.apps}
         * are preloaded on startup by invoking
         * {@link RegisteredAppRepository#save(RegisteredApp) save} here, which reaches
         * {@link AppAttestAppService}.
         */
        @Bean
        @ConditionalOnMissingBean(RegisteredAppRepository.class)
        @ConditionalOnBean(AppAttestAppService.class)
        public RegisteredAppRepository appleAppRepository(
                AppAttestAppService appAttestAppService,
                EulerSecurityAuthenticationAppAttestProperties properties) {
            return new AppAttestServiceRegisteredAppRepository(
                    appAttestAppService,
                    buildRegisteredApps(properties));
        }

        /**
         * In-memory fallback {@link RegisteredAppRepository} used when no
         * {@link AppAttestAppService} bean is available.
         */
        @Bean
        @ConditionalOnMissingBean({RegisteredAppRepository.class, AppAttestAppService.class})
        public RegisteredAppRepository inMemoryAppleAppRepository(
                EulerSecurityAuthenticationAppAttestProperties properties) {
            return new InMemoryRegisteredAppRepository(buildRegisteredApps(properties));
        }

        /**
         * Materialize the {@code euler.security.authentication.app-attest.apps} map into a list of
         * {@link RegisteredApp} instances suitable for preload.
         */
        private static List<RegisteredApp> buildRegisteredApps(
                EulerSecurityAuthenticationAppAttestProperties properties) {
            return properties.getApps().entrySet().stream()
                    .map(e -> RegisteredApp.withId(e.getKey())
                            .teamId(e.getValue().getTeamId())
                            .bundleId(e.getValue().getBundleId())
                            .oauth2Enabled(e.getValue().isOauth2Enabled())
                            .build())
                    .toList();
        }

        @Bean
        @ConditionalOnMissingBean(AppAttestAttestationRegistrationService.class)
        public AppAttestAttestationRegistrationService deviceAttestRegistrationService(JdbcOperations jdbcOperations) {
            return new JdbcAppAttestAttestationRegistrationService(jdbcOperations);
        }

        /**
         * Registry of the public keys an App instance issues and registers under itself as a
         * jwt-bearer assertion issuer, written by {@code POST /app_attest/keys}.
         * <p>
         * Distinct from {@link AppAttestAttestationRegistrationService}, which holds the App
         * Attest KEY the instance authenticates with: one proves the App instance, the other
         * vouches for a key its user signs with, and only the second has anything to do with
         * an account.
         */
        @Bean
        @ConditionalOnMissingBean(AppAttestIssuedKeyService.class)
        public AppAttestIssuedKeyService appAttestIssuedKeyService(JdbcOperations jdbcOperations) {
            return new JdbcAppAttestIssuedKeyService(jdbcOperations);
        }

        @Bean
        @ConditionalOnMissingBean(ChallengeService.class)
        public ChallengeService challengeService() {
            return new InMemoryChallengeService();
        }

        @Bean
        @ConditionalOnMissingBean(AppleAppAttestValidationService.class)
        public AppleAppAttestValidationService appleAppAttestValidationService(
                RegisteredAppRepository appleAppRepository,
                AppAttestAttestationRegistrationService registrationService,
                EulerSecurityAuthenticationAppAttestProperties properties) {
            // Default implementation: bundled Apple Root CA + pure-JDK X.509 / CBOR validation.
            // No webauthn4j dependency required. Applications that prefer the webauthn4j
            // implementation can register their own AppleAppAttestValidationService bean.
            DefaultAppleAppAttestValidationService defaultService =
                    new DefaultAppleAppAttestValidationService(appleAppRepository, registrationService);
            defaultService.setAllowDevelopmentEnvironment(properties.isDevelopmentEnvironment());
            return defaultService;
        }
    }

    /**
     * Autoconfiguration for the OTP module beans. Activated by
     * {@code euler.security.authentication.otp.enabled=true}.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.authentication.otp", name = "enabled", havingValue = "true")
    static class OneTimePasswordBeanConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OneTimePasswordGenerator oneTimePasswordGenerator() {
            return new SecureRandomOneTimePasswordGenerator();
        }

        @Bean
        @ConditionalOnMissingBean
        public OneTimePasswordPolicyResolver oneTimePasswordPolicyResolver(EulerSecurityAuthenticationOneTimePasswordProperties properties) {
            return new StaticOneTimePasswordPolicyResolver(properties.getPolicy().toOneTimePasswordPolicy());
        }

        // OneTimePasswordRecipientResolver: not provided by default. When the request carries
        // identity_id but no resolver bean is registered, the endpoint returns
        // invalid_identity_id.

        // ---- OneTimePasswordService: in-memory | jdbc | redis (mutually exclusive by storage) ----

        @Bean
        @ConditionalOnMissingBean(OneTimePasswordService.class)
        @ConditionalOnProperty(prefix = "euler.security.authentication.otp", name = "storage",
                havingValue = "in-memory", matchIfMissing = true)
        public OneTimePasswordService inMemoryOneTimePasswordService(OneTimePasswordGenerator oneTimePasswordGenerator,
                                                         EulerSecurityAuthenticationOneTimePasswordProperties properties) {
            return new InMemoryOneTimePasswordService(
                    oneTimePasswordGenerator,
                    InMemoryOneTimePasswordService.DEFAULT_MAX_TICKETS,
                    properties.getPolicy().getMaxFailures());
        }

        @Bean
        @ConditionalOnMissingBean(OneTimePasswordService.class)
        @ConditionalOnProperty(prefix = "euler.security.authentication.otp", name = "storage", havingValue = "jdbc")
        @ConditionalOnBean(JdbcOperations.class)
        public OneTimePasswordService jdbcOneTimePasswordService(OneTimePasswordGenerator oneTimePasswordGenerator,
                                                     JdbcOperations jdbcOperations,
                                                     EulerSecurityAuthenticationOneTimePasswordProperties properties) {
            return new JdbcOneTimePasswordService(
                    oneTimePasswordGenerator,
                    jdbcOperations,
                    JdbcOneTimePasswordService.DEFAULT_TABLE_NAME,
                    properties.getPolicy().getMaxFailures());
        }

        @Bean
        @ConditionalOnMissingBean(OneTimePasswordService.class)
        @ConditionalOnProperty(prefix = "euler.security.authentication.otp", name = "storage", havingValue = "redis")
        //@ConditionalOnBean(StringRedisTemplate.class)
        public OneTimePasswordService redisOneTimePasswordService(OneTimePasswordGenerator oneTimePasswordGenerator,
                                                      StringRedisTemplate redisTemplate,
                                                      EulerSecurityAuthenticationOneTimePasswordProperties properties) {
            return new RedisOneTimePasswordService(oneTimePasswordGenerator, redisTemplate, properties.getPolicy().getMaxFailures());
        }

        // ---- Channels ----
        // Boot only ships the stdout fallback. The actual OneTimePasswordChannel bean (typically a
        // DelegatingOneTimePasswordChannel composing business channels with stdout as fallback) is
        // expected to be assembled by the application; the routing table is a business
        // concern and must not be locked down by the framework.

        @Bean
        @ConditionalOnMissingBean
        public OneTimePasswordChannel stdoutOneTimePasswordChannel() {
            return new StdoutOneTimePasswordChannel();
        }
    }
}
