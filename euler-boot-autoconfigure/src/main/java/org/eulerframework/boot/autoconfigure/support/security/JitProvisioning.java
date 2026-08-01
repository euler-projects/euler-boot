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

/**
 * Just-in-time user provisioning settings, reusable by any
 * configuration section owning an authentication flow that may
 * encounter an unknown subject (federated login methods, WeChat login,
 * device attestation, the OTP grant).
 */
public class JitProvisioning {

    /**
     * Whether to provision a local user just in time on first
     * successful authentication when no local user matches. When false,
     * unknown subjects are rejected.
     */
    private boolean enabled = true;

    /**
     * Authorities granted to a just-in-time provisioned user. Declared
     * values replace the default; must not be empty while enabled.
     */
    private String[] defaultAuthorities = {"user"};

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getDefaultAuthorities() {
        return defaultAuthorities;
    }

    public void setDefaultAuthorities(String[] defaultAuthorities) {
        this.defaultAuthorities = defaultAuthorities;
    }
}
