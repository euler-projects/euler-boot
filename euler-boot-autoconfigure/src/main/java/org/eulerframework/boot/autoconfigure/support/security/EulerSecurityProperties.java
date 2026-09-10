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
public class EulerSecurityProperties {

    /**
     * Per-identity-type settings, keyed by the identity type stored as
     * t_user_identity.identity_type (e.g. "phone", "email", "google") or
     * by a pseudo identity type used by flows that establish no regular
     * identity ("device", "wechat"). An identity type without an entry
     * uses the defaults of every setting it owns.
     */
    private final Map<String, IdentityType> identityType = new LinkedHashMap<>();

    public Map<String, IdentityType> getIdentityType() {
        return identityType;
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
}
