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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "euler.security.oauth2.authorizationserver")
public class EulerAuthorizationServerProperties {
    private TokenStoreType authorizationStoreType = TokenStoreType.IN_MEMORY;
    private Duration authorizationLifetime = Duration.ofDays(7);
    private String redisKeyPrefix = "euler:oauth2:auth";

    private String consentPage;

    private final DynamicClientRegistration dynamicClientRegistration = new DynamicClientRegistration();

    private final Endpoint endpoint = new Endpoint();

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

    public Endpoint getEndpoint() {
        return endpoint;
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

    /**
     * Endpoint URIs this framework adds to the authorization server, mirroring the shape of Spring's
     * {@code spring.security.oauth2.authorizationserver.endpoint}.
     */
    public static class Endpoint {
        /**
         * URI of the challenge endpoint that issues the fresh challenges clients include in their
         * attestation. Default is {@code /oauth2/challenge}.
         */
        private String challengeUri = "/oauth2/challenge";

        /**
         * URI of the Authorization Challenge Endpoint (draft-ietf-oauth-first-party-apps), which
         * exchanges an {@code otp_ticket} + {@code otp} user authentication for an
         * {@code authorization_code}. Default is {@code /authorize-challenge}. The endpoint has no
         * dedicated toggle: it is wired wherever attestation-based client authentication and the OTP
         * module are both enabled, since it reuses both.
         */
        private String authorizationChallengeUri = "/authorize-challenge";

        public String getChallengeUri() {
            return challengeUri;
        }

        public void setChallengeUri(String challengeUri) {
            this.challengeUri = challengeUri;
        }

        public String getAuthorizationChallengeUri() {
            return authorizationChallengeUri;
        }

        public void setAuthorizationChallengeUri(String authorizationChallengeUri) {
            this.authorizationChallengeUri = authorizationChallengeUri;
        }
    }
}
