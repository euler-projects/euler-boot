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

import org.eulerframework.boot.autoconfigure.support.data.jpa.EulerBootDataJpaAuditingAutoConfiguration;
import org.eulerframework.security.authentication.ChallengeService;
import org.eulerframework.security.authentication.InMemoryChallengeService;
import org.eulerframework.security.authentication.appattest.apple.AppleAppAttestValidationService;
import org.eulerframework.security.authentication.appattest.apple.DefaultAppleAppAttestValidationService;
import org.eulerframework.security.authentication.appattest.*;
import org.eulerframework.security.authentication.otp.InMemoryOtpTicketService;
import org.eulerframework.security.authentication.otp.JdbcOtpTicketService;
import org.eulerframework.security.authentication.otp.OtpChannel;
import org.eulerframework.security.authentication.otp.OtpGenerator;
import org.eulerframework.security.authentication.otp.OtpPolicyResolver;
import org.eulerframework.security.authentication.otp.OtpTicketService;
import org.eulerframework.security.authentication.otp.RedisOtpTicketService;
import org.eulerframework.security.authentication.otp.SecureRandomOtpGenerator;
import org.eulerframework.security.authentication.otp.StaticOtpPolicyResolver;
import org.eulerframework.security.authentication.otp.StdoutOtpChannel;
import org.eulerframework.security.oauth2.server.authorization.client.AppAttestOAuth2ClientProvisioningListener;
import org.eulerframework.security.webauthn.authentication.AppleAppAttestRootCA;
import com.webauthn4j.appattest.DeviceCheckManager;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.context.UserDetailsPrincipalUserContext;
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
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Collections;
import java.util.List;

@AutoConfiguration(
        before = {
                EulerBootDataJpaAuditingAutoConfiguration.class
        },
        beforeName = {
                "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
                "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration"
        })
@EnableConfigurationProperties({
        EulerBootSecurityProperties.class,
        EulerBootSecurityAppAttestProperties.class,
        EulerBootSecurityOtpProperties.class
})
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
public class EulerBootSecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        return new UserDetailsPrincipalUserContext();
    }

    @Bean
    @ConditionalOnProperty(prefix = "euler.security.oauth2.authorizationserver.wechat-login", name = "enabled")
    static public InitializeWechatUserDetailsBeanManagerConfigurer initializeWechatLoginBeanManagerConfigurer(ApplicationContext context) {
        return new InitializeWechatUserDetailsBeanManagerConfigurer(context);
    }

    /**
     * Autoconfiguration for App Attest related beans.
     * Creates default implementations of required services when no custom beans are provided.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.app-attest", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "com.webauthn4j.appattest.DeviceCheckManager")
    static class DeviceAttestBeanConfiguration {

        /**
         * Service-backed {@link RegisteredAppRepository} used when an
         * {@link AppAttestAppService} bean is present in the context. Listener fan-out
         * for this path is handled inside the service layer &mdash; this bean is a bare
         * bridge and does not wrap itself in any notification decorator. Apps declared
         * under {@code euler.security.app-attest.apps} are preloaded on startup by
         * invoking {@link RegisteredAppRepository#save(RegisteredApp) save} here, which
         * reaches {@link AppAttestAppService} and triggers its in-service notification.
         */
        @Bean
        @ConditionalOnMissingBean(RegisteredAppRepository.class)
        @ConditionalOnBean(AppAttestAppService.class)
        public RegisteredAppRepository appleAppRepository(
                AppAttestAppService appAttestAppService,
                EulerBootSecurityAppAttestProperties properties) {
            return new AppAttestServiceRegisteredAppRepository(
                    appAttestAppService,
                    buildRegisteredApps(properties));
        }

        /**
         * In-memory fallback {@link RegisteredAppRepository} used when no
         * {@link AppAttestAppService} bean is available. Listeners are passed into the
         * repository constructor so that preloaded apps and any subsequent runtime save
         * both dispatch {@link RegisteredAppChangeListener#onRegisteredAppSaved}.
         */
        @Bean
        @ConditionalOnMissingBean({RegisteredAppRepository.class, AppAttestAppService.class})
        public RegisteredAppRepository inMemoryAppleAppRepository(
                EulerBootSecurityAppAttestProperties properties,
                List<RegisteredAppChangeListener> listeners) {
            return new InMemoryRegisteredAppRepository(
                    buildRegisteredApps(properties),
                    listeners == null ? Collections.emptyList() : listeners);
        }

        /**
         * Materialize the {@code euler.security.app-attest.apps} map into a list of
         * {@link RegisteredApp} instances suitable for preload.
         */
        private static List<RegisteredApp> buildRegisteredApps(
                EulerBootSecurityAppAttestProperties properties) {
            return properties.getApps().entrySet().stream()
                    .map(e -> RegisteredApp.withId(e.getKey())
                            .teamId(e.getValue().getTeamId())
                            .bundleId(e.getValue().getBundleId())
                            .oauth2Enabled(e.getValue().isOauth2Enabled())
                            .oauth2ClientType(e.getValue().getOauth2ClientType())
                            .build())
                    .toList();
        }

        @Bean
        @ConditionalOnMissingBean(DeviceCheckManager.class)
        public DeviceCheckManager deviceCheckManager(EulerBootSecurityAppAttestProperties properties) {
            DeviceCheckManager deviceCheckManager = AppleAppAttestRootCA.deviceCheckManager();
            deviceCheckManager.getAttestationDataValidator().setProduction(!properties.isDevelopmentEnvironment());
            return deviceCheckManager;
        }

        @Bean
        @ConditionalOnMissingBean(AppAttestAttestationRegistrationService.class)
        public AppAttestAttestationRegistrationService deviceAttestRegistrationService(JdbcOperations jdbcOperations) {
            return new JdbcAppAttestAttestationRegistrationService(jdbcOperations);
        }

        @Bean
        @ConditionalOnMissingBean(ChallengeService.class)
        public ChallengeService challengeService() {
            return new InMemoryChallengeService();
        }

        @Bean
        @ConditionalOnMissingBean(AppleAppAttestValidationService.class)
        public AppleAppAttestValidationService appleAppAttestValidationService(
                DeviceCheckManager deviceCheckManager,
                RegisteredAppRepository appleAppRepository,
                AppAttestAttestationRegistrationService registrationService,
                EulerBootSecurityAppAttestProperties properties) {
            DefaultAppleAppAttestValidationService defaultAppleAppAttestValidationService = new DefaultAppleAppAttestValidationService(appleAppRepository, registrationService);
            defaultAppleAppAttestValidationService.setAllowDevelopmentEnvironment(properties.isDevelopmentEnvironment());
            return defaultAppleAppAttestValidationService;
            //            return new Webauthn4jAppleAppAttestValidationService(deviceCheckManager, appleAppRepository, registrationService,
//                    properties.isAllowDevelopmentEnvironment());
        }
    }

    /**
     * Autoconfiguration for the OTP module beans. Activated by
     * {@code euler.security.otp.enabled=true}.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.otp", name = "enabled", havingValue = "true")
    static class OtpBeanConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OtpGenerator otpGenerator() {
            return new SecureRandomOtpGenerator();
        }

        @Bean
        @ConditionalOnMissingBean
        public OtpPolicyResolver otpPolicyResolver(EulerBootSecurityOtpProperties properties) {
            return new StaticOtpPolicyResolver(properties.getPolicy().toOtpPolicy());
        }

        // OtpRecipientResolver: not provided by default. When the request carries
        // identity_id but no resolver bean is registered, the endpoint returns
        // invalid_identity_id.

        // ---- OtpTicketService: in-memory | jdbc | redis (mutually exclusive by storage) ----

        @Bean
        @ConditionalOnMissingBean(OtpTicketService.class)
        @ConditionalOnProperty(prefix = "euler.security.otp", name = "storage",
                havingValue = "in-memory", matchIfMissing = true)
        public OtpTicketService inMemoryOtpTicketService(EulerBootSecurityOtpProperties properties) {
            return new InMemoryOtpTicketService(
                    InMemoryOtpTicketService.DEFAULT_MAX_TICKETS,
                    properties.getPolicy().getMaxFailures());
        }

        @Bean
        @ConditionalOnMissingBean(OtpTicketService.class)
        @ConditionalOnProperty(prefix = "euler.security.otp", name = "storage", havingValue = "jdbc")
        @ConditionalOnBean(JdbcOperations.class)
        public OtpTicketService jdbcOtpTicketService(JdbcOperations jdbcOperations,
                                                     EulerBootSecurityOtpProperties properties) {
            return new JdbcOtpTicketService(
                    jdbcOperations,
                    JdbcOtpTicketService.DEFAULT_TABLE_NAME,
                    properties.getPolicy().getMaxFailures());
        }

        @Bean
        @ConditionalOnMissingBean(OtpTicketService.class)
        @ConditionalOnProperty(prefix = "euler.security.otp", name = "storage", havingValue = "redis")
        //@ConditionalOnBean(StringRedisTemplate.class)
        public OtpTicketService redisOtpTicketService(StringRedisTemplate redisTemplate,
                                                      EulerBootSecurityOtpProperties properties) {
            return new RedisOtpTicketService(redisTemplate, properties.getPolicy().getMaxFailures());
        }

        // ---- Channels ----
        // Boot only ships the stdout fallback. The actual OtpChannel bean (typically a
        // DelegatingOtpChannel composing business channels with stdout as fallback) is
        // expected to be assembled by the application; the routing table is a business
        // concern and must not be locked down by the framework.

        @Bean
        @ConditionalOnMissingBean(name = "stdoutOtpChannel")
        public OtpChannel stdoutOtpChannel() {
            return new StdoutOtpChannel();
        }
    }

    /**
     * Autoconfiguration for provisioning OAuth2 clients from registered apps.
     * <p>
     * This configuration class is separate from {@link DeviceAttestBeanConfiguration} to
     * ensure the provisioning listener bean is created before the {@link RegisteredAppRepository}
     * bean, so that the listener is available for injection when the repository is initialized.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.app-attest", name = "enabled", havingValue = "true")
    @ConditionalOnBean(RegisteredClientRepository.class)
    static class AppAttestOAuth2ProvisioningConfiguration {

        @Bean
        public AppAttestOAuth2ClientProvisioningListener appAttestOAuth2ClientProvisioningListener(
                RegisteredClientRepository registeredClientRepository) {
            return new AppAttestOAuth2ClientProvisioningListener(registeredClientRepository);
        }
    }
}
