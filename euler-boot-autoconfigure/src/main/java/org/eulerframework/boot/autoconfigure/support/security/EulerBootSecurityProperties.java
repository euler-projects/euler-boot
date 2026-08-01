/*
 * Copyright 2013-2024 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "euler.security")
public class EulerBootSecurityProperties {

    /**
     * Per-identity-type settings, keyed by the identity type stored as
     * t_user_identity.identity_type (e.g. "phone", "email", "google") or
     * by a pseudo identity type used by flows that establish no regular
     * identity ("device", "wechat"). An identity type without an entry
     * uses the defaults of every setting it owns.
     */
    private final Map<String, IdentityType> identityType = new LinkedHashMap<>();

    /**
     * Declared login options, keyed by the name each method is
     * registered under. This is the frontend-agnostic catalog of login
     * methods; the built-in login page is only one consumer, an SPA or
     * native client may consume the same registry. When empty, a single
     * primary password method is assumed.
     */
    private final Map<String, LoginMethod> loginMethod = new LinkedHashMap<>();

    public Map<String, IdentityType> getIdentityType() {
        return identityType;
    }

    public Map<String, LoginMethod> getLoginMethod() {
        return loginMethod;
    }

    /**
     * Settings owned by a single identity type.
     */
    public static class IdentityType {

        /**
         * Just-in-time provisioning applied when an identity of this
         * type is seen for the first time, regardless of the entry point
         * (web login, OAuth2 token grant, device registration).
         */
        @NestedConfigurationProperty
        private final JitProvisioning jitProvisioning = new JitProvisioning();

        public JitProvisioning getJitProvisioning() {
            return jitProvisioning;
        }
    }

    /**
     * A single login-method declaration under
     * {@code euler.security.login-method.<key>}. The key is a
     * registry-local identifier and is never shown to users.
     *
     * <p>Top-level properties carry the {@code method-type}
     * discriminator and the cross-type login policy; the
     * {@code properties} bag carries optional type-specific settings
     * interpreted by the login-method handler registered for the
     * declared type.
     */
    public static class LoginMethod {

        /**
         * Type discriminator selecting the handler that serves this
         * method, e.g. "password", "oauth2", "otp".
         */
        private String methodType;

        /**
         * Name identifying this method towards clients, unique across
         * all declared methods. When omitted, the handler derives one
         * (e.g. the OAuth2 provider or the OTP channel); declare it to
         * resolve derivation conflicts or to pick a custom name.
         */
        private String methodName;

        /**
         * Identity type established by this login method, stored as
         * t_user_identity.identity_type. Also the semantic anchor
         * handlers derive from, such as the OAuth2 provider or the OTP
         * delivery channel.
         */
        private String identityType;

        /**
         * Whether this method belongs to the expanded (primary) group of
         * the login page rather than the secondary group. Several
         * methods may be primary; when none declares it, the first
         * declared method is treated as primary.
         */
        private boolean primary = false;

        /**
         * Type-specific settings, interpreted solely by the handler
         * registered for the declared type.
         */
        private final Map<String, Object> properties = new LinkedHashMap<>();

        public String getMethodType() {
            return methodType;
        }

        public void setMethodType(String methodType) {
            this.methodType = methodType;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setMethodName(String methodName) {
            this.methodName = methodName;
        }

        public String getIdentityType() {
            return identityType;
        }

        public void setIdentityType(String identityType) {
            this.identityType = identityType;
        }

        public boolean isPrimary() {
            return primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }
    }
}
