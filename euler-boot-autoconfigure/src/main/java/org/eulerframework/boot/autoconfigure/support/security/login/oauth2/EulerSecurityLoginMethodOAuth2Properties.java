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
package org.eulerframework.boot.autoconfigure.support.security.login.oauth2;

import org.eulerframework.boot.autoconfigure.support.security.login.BaseLoginMethodConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "euler.security.login-method")
public class EulerSecurityLoginMethodOAuth2Properties {
    private final Map<String, LoginMethodOAuth2Configuration> oauth2 = new LinkedHashMap<>();

    public Map<String, LoginMethodOAuth2Configuration> getOauth2() {
        return oauth2;
    }

    /**
     * An {@code oauth2} method declaration.
     */
    public static class LoginMethodOAuth2Configuration extends BaseLoginMethodConfiguration {

        /**
         * Identity type established on a successful sign-in, stored
         * as t_user_identity.identity_type, e.g. "google". Also the
         * fallback for the provider.
         */
        private String identityType;

        /**
         * Provider this method federates to, published to clients so
         * that they can brand the option. Defaults to the identity
         * type; declare it when several methods federate to the same
         * provider under different identity types.
         */
        private String provider;

        /**
         * Registration under
         * spring.security.oauth2.client.registration to drive the
         * flow with. Defaults to the provider, which covers one
         * registration per provider.
         */
        private String oauthClientRegistrationId;

        public String getIdentityType() {
            return identityType;
        }

        public void setIdentityType(String identityType) {
            this.identityType = identityType;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getOauthClientRegistrationId() {
            return oauthClientRegistrationId;
        }

        public void setOauthClientRegistrationId(String oauthClientRegistrationId) {
            this.oauthClientRegistrationId = oauthClientRegistrationId;
        }
    }
}
