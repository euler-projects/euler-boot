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

import org.eulerframework.security.authentication.apple.AppleAppAttestAssertionAuthenticationProvider;
import org.eulerframework.security.authentication.apple.AppleAppAttestValidationService;
import org.eulerframework.security.authentication.apple.AppleAppRepository;
import org.eulerframework.security.core.userdetails.EulerAppleAppAttestUserDetailsService;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "euler.security.oauth2.authorizationserver")
public class EulerBootAuthorizationServerProperties {
    private TokenStoreType authorizationStoreType = TokenStoreType.IN_MEMORY;
    private Duration authorizationLifetime = Duration.ofDays(7);
    private String redisKeyPrefix = "euler:oauth2:auth";

    private String consentPage;

    private DynamicClientRegistration dynamicClientRegistration =  new DynamicClientRegistration();

    private WechatLogin wechatLogin = new WechatLogin();

    private AppleAppAttest appleAppAttest = new AppleAppAttest();

    public TokenStoreType getAuthorizationStoreType() {
        return authorizationStoreType;
    }

    public void setAuthorizationStoreType(TokenStoreType authorizationStoreType) {
        this.authorizationStoreType = authorizationStoreType;
    }

    public Duration getAuthorizationLifetime() {
        return authorizationLifetime;
    }

    public void setAuthorizationLifetime(Duration authorizationLifetime) {
        this.authorizationLifetime = authorizationLifetime;
    }

    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }

    public String getConsentPage() {
        return consentPage;
    }

    public void setConsentPage(String consentPage) {
        this.consentPage = consentPage;
    }

    public enum TokenStoreType {
        JDBC, REDIS, IN_MEMORY
    }

    public DynamicClientRegistration getDynamicClientRegistration() {
        return dynamicClientRegistration;
    }

    public WechatLogin getWechatLogin() {
        return wechatLogin;
    }

    public void setWechatLogin(WechatLogin wechatLogin) {
        this.wechatLogin = wechatLogin;
    }

    public AppleAppAttest getAppleAppAttest() {
        return appleAppAttest;
    }

    public void setAppleAppAttest(AppleAppAttest appleAppAttest) {
        this.appleAppAttest = appleAppAttest;
    }

    /**
     * Apple App Attest validation configuration.
     * <p>
     * When enabled, registers {@link AppleAppAttestAssertionAuthenticationProvider
     * AppleAppAttestAssertionAuthenticationProvider} into the {@code AuthenticationManager}
     * to support Apple App Attest assertion grant type.
     * <p>
     * Requires an {@link EulerAppleAppAttestUserDetailsService
     * EulerAppleAppAttestUserDetailsService} bean to be present in the application context.
     * If no {@link AppleAppAttestValidationService
     * AppleAppAttestValidationService} bean is found, a default implementation will be
     * created using the registered apps from {@link #apps}.
     */
    public static class AppleAppAttest {
        /**
         * Whether Apple App Attest authentication is enabled. Default is {@code false}.
         */
        private boolean enabled;
        /**
         * Whether to accept attestations from the development environment.
         * When {@code true}, both production and development AAGUIDs are accepted.
         * When {@code false} (default), only the production AAGUID is accepted.
         * <p>
         * Should only be set to {@code true} during development and testing.
         */
        private boolean allowDevelopmentEnvironment;
        /**
         * Map of registered Apple Apps, keyed by an arbitrary registration name.
         * Each entry defines a {@link AppRegistration} with a {@code teamId} and {@code bundleId}
         * that together form the App ID ({@code teamId.bundleId}) for RP ID hash verification.
         * <p>
         * Required when no custom {@link AppleAppRepository AppleAppRepository} or
         * {@link AppleAppAttestValidationService AppleAppAttestValidationService} bean is provided.
         */
        private Map<String, AppRegistration> apps = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAllowDevelopmentEnvironment() {
            return allowDevelopmentEnvironment;
        }

        public void setAllowDevelopmentEnvironment(boolean allowDevelopmentEnvironment) {
            this.allowDevelopmentEnvironment = allowDevelopmentEnvironment;
        }

        public Map<String, AppRegistration> getApps() {
            return apps;
        }

        public void setApps(Map<String, AppRegistration> apps) {
            this.apps = apps;
        }

        /**
         * Registration properties for a single Apple App.
         */
        public static class AppRegistration {
            /**
             * The Apple Developer Team ID (10-character string), used together with
             * {@link #bundleId} to compute the App ID ({@code teamId.bundleId}) for
             * RP ID hash verification.
             */
            private String teamId;
            /**
             * The iOS app's Bundle ID (e.g. {@code com.example.myapp}), used together with
             * {@link #teamId} to compute the App ID for RP ID hash verification.
             */
            private String bundleId;

            public String getTeamId() {
                return teamId;
            }

            public void setTeamId(String teamId) {
                this.teamId = teamId;
            }

            public String getBundleId() {
                return bundleId;
            }

            public void setBundleId(String bundleId) {
                this.bundleId = bundleId;
            }
        }
    }

    public static class WechatLogin {
        private boolean enabled;
        private boolean autoCreateUserIfNotExists;
        private String code2SessionEndpoint = "https://api.weixin.qq.com/sns/jscode2session";
        private String appid;
        private String secret;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAutoCreateUserIfNotExists() {
            return autoCreateUserIfNotExists;
        }

        public void setAutoCreateUserIfNotExists(boolean autoCreateUserIfNotExists) {
            this.autoCreateUserIfNotExists = autoCreateUserIfNotExists;
        }

        public String getCode2SessionEndpoint() {
            return code2SessionEndpoint;
        }

        public void setCode2SessionEndpoint(String code2SessionEndpoint) {
            this.code2SessionEndpoint = code2SessionEndpoint;
        }

        public String getAppid() {
            return appid;
        }

        public void setAppid(String appid) {
            this.appid = appid;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    public static class DynamicClientRegistration {
        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
