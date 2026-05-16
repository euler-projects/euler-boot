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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the user authentication factor module
 * ({@code /user/identities} REST surface).
 *
 * <pre>
 * euler:
 *   security:
 *     authentication-factor:
 *       enabled: true
 *       endpoint-base-uri: /user/identities
 * </pre>
 */
@ConfigurationProperties(prefix = "euler.security.authentication-factor")
public class EulerBootSecurityAuthenticationFactorProperties {

    /**
     * Whether the user authentication factor module is enabled. Default is
     * {@code true}; the filter is only attached to the authorization-server
     * filter chain when at least one factor-specific
     * {@code UserAuthenticationService} bean is registered, so leaving this
     * on is safe even when no factor service is available.
     */
    private boolean enabled = true;

    /**
     * Base URI of the {@code /user/identities} endpoint family. Default is
     * {@code /user/identities}, kept aligned with the existing client v2
     * contract.
     */
    private String endpointBaseUri = "/user/identities";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpointBaseUri() {
        return endpointBaseUri;
    }

    public void setEndpointBaseUri(String endpointBaseUri) {
        this.endpointBaseUri = endpointBaseUri;
    }
}
