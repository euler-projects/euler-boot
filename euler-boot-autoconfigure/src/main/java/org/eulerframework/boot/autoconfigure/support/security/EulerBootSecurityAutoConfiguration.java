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
import org.eulerframework.security.authentication.apple.AppleAppAttestValidationService;
import org.eulerframework.security.authentication.apple.DefaultAppleAppAttestValidationService;
import org.eulerframework.security.authentication.device.*;
import org.eulerframework.security.webauthn.authentication.AppleAppAttestRootCA;
import com.webauthn4j.appattest.DeviceCheckManager;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.core.context.UserDetailsPrincipalUserContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

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
        EulerBootSecurityDeviceAttestProperties.class
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
     * Auto-configuration for Device Attest related beans.
     * Creates default implementations of required services when no custom beans are provided.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.device-attest", name = "enabled", havingValue = "true")
    @ConditionalOnClass(name = "com.webauthn4j.appattest.DeviceCheckManager")
    static class DeviceAttestBeanConfiguration {

        @Bean
        @ConditionalOnMissingBean(DeviceRepository.class)
        public DeviceRepository appleAppRepository(EulerBootSecurityDeviceAttestProperties properties) {
            List<RegisteredDevice> registeredApps = properties.getAllowedDevices().stream()
                    .map(app -> new RegisteredDevice(app.getTeamId(), app.getBundleId()))
                    .toList();
            return new InMemoryDeviceRepository(registeredApps);
        }

        @Bean
        @ConditionalOnMissingBean(DeviceCheckManager.class)
        public DeviceCheckManager deviceCheckManager(EulerBootSecurityDeviceAttestProperties properties) {
            DeviceCheckManager deviceCheckManager = AppleAppAttestRootCA.deviceCheckManager();
            deviceCheckManager.getAttestationDataValidator().setProduction(!properties.isAllowDevelopmentEnvironment());
            return deviceCheckManager;
        }

        @Bean
        @ConditionalOnMissingBean(DeviceAttestationRegistrationService.class)
        public DeviceAttestationRegistrationService deviceAttestRegistrationService(JdbcOperations jdbcOperations) {
            return new JdbcDeviceAttestationRegistrationService(jdbcOperations);
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
                DeviceRepository appleAppRepository,
                DeviceAttestationRegistrationService registrationService,
                EulerBootSecurityDeviceAttestProperties properties) {
            DefaultAppleAppAttestValidationService defaultAppleAppAttestValidationService = new DefaultAppleAppAttestValidationService(appleAppRepository, registrationService);
            defaultAppleAppAttestValidationService.setAllowDevelopmentEnvironment(properties.isAllowDevelopmentEnvironment());
            return defaultAppleAppAttestValidationService;
            //            return new Webauthn4jAppleAppAttestValidationService(deviceCheckManager, appleAppRepository, registrationService,
//                    properties.isAllowDevelopmentEnvironment());
        }
    }
}
