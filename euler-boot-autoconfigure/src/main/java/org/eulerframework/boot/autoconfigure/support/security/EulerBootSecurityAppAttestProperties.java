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

import org.eulerframework.security.authentication.appattest.RegisteredApp;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration properties for device attestation registration and assertion.
 * <p>
 * These properties control the device attest endpoint configuration including
 * the list of allowed apps (teamId + bundleId pairs) and environment settings.
 *
 * <pre>
 * euler:
 *   security:
 *     app-attest:
 *       enabled: true
 *       apps:
 *         myapp:
 *           team-id: ABCDE12345
 *           bundle-id: com.example.myapp
 *         myapp-dev:
 *           team-id: ABCDE12345
 *           bundle-id: com.example.myapp.dev
 *           oauth2-enabled: true
 *           oauth2-client-type: static
 *       development-environment: false
 * </pre>
 */
@ConfigurationProperties(prefix = "euler.security.app-attest")
public class EulerBootSecurityAppAttestProperties {

    /**
     * Whether device attestation is enabled. Default is {@code false}.
     */
    private boolean enabled = false;

    /**
     * Map of allowed apps, keyed by a logical app name chosen by the operator.
     * <p>
     * The map key is both a human-readable identifier used in configuration (e.g.
     * {@code myapp}, {@code myapp-dev}) and the {@code registrationId} of the
     * corresponding registered app, uniquely identifying this registration record.
     * Accordingly, {@link App} itself does not duplicate an id field; the canonical
     * app identity is still derived from {@code teamId} + {@code bundleId}.
     */
    private Map<String, App> apps = new LinkedHashMap<>();

    /**
     * Whether to accept attestations from the development environment.
     * When {@code true}, both production and development AAGUIDs are accepted.
     * When {@code false} (default), only the production AAGUID is accepted.
     * <p>
     * Should only be set to {@code true} during development and testing.
     */
    private boolean developmentEnvironment = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Map<String, App> getApps() {
        return apps;
    }

    public void setApps(Map<String, App> apps) {
        this.apps = apps;
    }

    public boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }

    public void setDevelopmentEnvironment(boolean developmentEnvironment) {
        this.developmentEnvironment = developmentEnvironment;
    }

    /**
     * Properties for a single allowed App.
     * <p>
     * This type does not carry its own id field &mdash; the containing map key serves
     * as this app's {@code registrationId}, uniquely identifying the registration
     * record.
     */
    public static class App {
        /**
         * The Apple Developer Team ID (10-character string).
         */
        private String teamId;

        /**
         * The app's Bundle ID (e.g. {@code com.example.myapp}).
         */
        private String bundleId;

        /**
         * Whether this app may act as an OAuth2 client and request access tokens from the
         * authorization server. Default is {@code false}.
         */
        private boolean oauth2Enabled = false;

        /**
         * How the OAuth2 {@code client_id} is provisioned. Required when
         * {@link #oauth2Enabled} is {@code true}.
         *
         * @see RegisteredApp.OAuth2ClientType
         */
        private RegisteredApp.OAuth2ClientType oauth2ClientType;

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

        public boolean isOauth2Enabled() {
            return oauth2Enabled;
        }

        public void setOauth2Enabled(boolean oauth2Enabled) {
            this.oauth2Enabled = oauth2Enabled;
        }

        public RegisteredApp.OAuth2ClientType getOauth2ClientType() {
            return oauth2ClientType;
        }

        public void setOauth2ClientType(RegisteredApp.OAuth2ClientType oauth2ClientType) {
            this.oauth2ClientType = oauth2ClientType;
        }
    }
}
