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
package org.eulerframework.boot.autoconfigure.support.security.login.otp;

import org.eulerframework.boot.autoconfigure.support.security.login.BaseLoginMethodConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "euler.security.login-method")
public class EulerSecurityLoginMethodOtpProperties {
    private final Map<String, LoginMethodOtpConfiguration> otp = new LinkedHashMap<>();

    public Map<String, LoginMethodOtpConfiguration> getOtp() {
        return otp;
    }

    /**
     * An {@code otp} method declaration.
     */
    public static class LoginMethodOtpConfiguration extends BaseLoginMethodConfiguration {

        /**
         * Identity type the one-time password is sent to and
         * established on a successful sign-in, stored as
         * t_user_identity.identity_type, e.g. "phone" or "email".
         * Also the fallback for the channel.
         */
        private String identityType;

        /**
         * Channel the one-time password is delivered over, e.g.
         * "sms". Defaults to the canonical channel of the identity
         * type (phone to sms, email to email).
         */
        private String channel;

        public String getIdentityType() {
            return identityType;
        }

        public void setIdentityType(String identityType) {
            this.identityType = identityType;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }
    }
}
