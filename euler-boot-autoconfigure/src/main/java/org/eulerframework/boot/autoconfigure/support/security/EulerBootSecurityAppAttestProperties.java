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

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Apple App Attest device registration and assertion.
 * <p>
 * These properties control the App Attest endpoint configuration including
 * the list of allowed apps (teamId + bundleId pairs) and environment settings.
 *
 * <pre>
 * euler:
 *   security:
 *     app-attest:
 *       enabled: true
 *       allowed-apps:
 *         - team-id: ABCDE12345
 *           bundle-id: com.example.myapp
 *         - team-id: ABCDE12345
 *           bundle-id: com.example.myapp.dev
 *       allow-development-environment: false
 * </pre>
 */
@ConfigurationProperties(prefix = "euler.security.app-attest")
public class EulerBootSecurityAppAttestProperties {

    /**
     * Whether Apple App Attest is enabled. Default is {@code false}.
     */
    private boolean enabled = false;

    /**
     * List of allowed Apple Apps. Each entry defines a teamId + bundleId pair
     * that together form the App ID ({@code teamId.bundleId}) for RP ID hash verification.
     */
    private List<AllowedApp> allowedApps = new ArrayList<>();

    /**
     * Whether to accept attestations from the development environment.
     * When {@code true}, both production and development AAGUIDs are accepted.
     * When {@code false} (default), only the production AAGUID is accepted.
     * <p>
     * Should only be set to {@code true} during development and testing.
     */
    private boolean allowDevelopmentEnvironment = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<AllowedApp> getAllowedApps() {
        return allowedApps;
    }

    public void setAllowedApps(List<AllowedApp> allowedApps) {
        this.allowedApps = allowedApps;
    }

    public boolean isAllowDevelopmentEnvironment() {
        return allowDevelopmentEnvironment;
    }

    public void setAllowDevelopmentEnvironment(boolean allowDevelopmentEnvironment) {
        this.allowDevelopmentEnvironment = allowDevelopmentEnvironment;
    }

    /**
     * Properties for a single allowed Apple App.
     */
    public static class AllowedApp {
        /**
         * The Apple Developer Team ID (10-character string).
         */
        private String teamId;

        /**
         * The iOS app's Bundle ID (e.g. {@code com.example.myapp}).
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
