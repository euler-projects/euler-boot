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
        EulerBootSecurityAppAttestProperties.class
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
