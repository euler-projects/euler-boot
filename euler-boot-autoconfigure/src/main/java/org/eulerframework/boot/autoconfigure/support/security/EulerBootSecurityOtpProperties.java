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

import org.eulerframework.security.authentication.otp.OtpPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the OTP module.
 *
 * <pre>
 * euler:
 *   security:
 *     otp:
 *       enabled: false
 *       issue-endpoint-uri: /otp/tickets
 *       storage: in-memory          # in-memory | jdbc | redis
 *       pkce:
 *         enabled: false            # PKCE (RFC 7636) is opt-in; default OFF
 *       policy:
 *         otp-length: 6
 *         expires-in: 5m
 *         retry-after: 60s
 *         max-failures: 5
 * </pre>
 */
@ConfigurationProperties(prefix = "euler.security.otp")
public class EulerBootSecurityOtpProperties {

    /**
     * Whether the OTP module is enabled. Default is {@code false}.
     */
    private boolean enabled = false;

    /**
     * URI of the OTP ticket issue endpoint. Default is {@code /otp/tickets}.
     */
    private String issueEndpointUri = "/otp/tickets";

    /**
     * Storage backend for {@link org.eulerframework.security.authentication.otp.OtpTicketService OtpTicketService}.
     * Default is {@link Storage#IN_MEMORY}.
     */
    private Storage storage = Storage.IN_MEMORY;

    /**
     * PKCE (RFC 7636) settings. Disabled by default; when disabled, neither
     * the {@code POST /otp/tickets} issue endpoint nor the
     * {@code grant_type=otp} token endpoint accepts (or requires) PKCE
     * parameters.
     */
    private Pkce pkce = new Pkce();

    /**
     * Default OTP policy (the framework only ships a single global policy; per
     * channel / purpose / identity differentiation is a business concern handled
     * by providing a custom {@link org.eulerframework.security.authentication.otp.OtpPolicyResolver OtpPolicyResolver} bean).
     */
    private Policy policy = new Policy();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssueEndpointUri() {
        return issueEndpointUri;
    }

    public void setIssueEndpointUri(String issueEndpointUri) {
        this.issueEndpointUri = issueEndpointUri;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Pkce getPkce() {
        return pkce;
    }

    public void setPkce(Pkce pkce) {
        this.pkce = pkce;
    }

    public enum Storage {
        IN_MEMORY,
        JDBC,
        REDIS
    }

    /**
     * PKCE switch carrier.
     */
    public static class Pkce {

        /**
         * Whether PKCE (RFC 7636) is required for OTP. When {@code false}
         * (default), {@code code_challenge} / {@code code_challenge_method}
         * on the issue endpoint and {@code code_verifier} on the token
         * endpoint are neither required nor consulted.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * Properties carrier for {@link OtpPolicy}.
     */
    public static class Policy {

        /**
         * OTP value length (number of digits). Default is {@code 6}.
         */
        private int otpLength = 6;

        /**
         * Time-to-live of an issued OTP ticket. Default is 5 minutes.
         */
        private Duration expiresIn = Duration.ofMinutes(5);

        /**
         * Hint to clients about the minimum interval between subsequent re-issues.
         * The framework does not enforce this in this release. Default is 60 seconds.
         */
        private Duration retryAfter = Duration.ofSeconds(60);

        /**
         * Maximum number of failed verification attempts before a ticket is invalidated.
         * Default is {@code 5}.
         */
        private int maxFailures = 5;

        public int getOtpLength() {
            return otpLength;
        }

        public void setOtpLength(int otpLength) {
            this.otpLength = otpLength;
        }

        public Duration getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Duration expiresIn) {
            this.expiresIn = expiresIn;
        }

        public Duration getRetryAfter() {
            return retryAfter;
        }

        public void setRetryAfter(Duration retryAfter) {
            this.retryAfter = retryAfter;
        }

        public int getMaxFailures() {
            return maxFailures;
        }

        public void setMaxFailures(int maxFailures) {
            this.maxFailures = maxFailures;
        }

        public OtpPolicy toOtpPolicy() {
            return new OtpPolicy(this.otpLength, this.expiresIn, this.retryAfter, this.maxFailures);
        }
    }
}
