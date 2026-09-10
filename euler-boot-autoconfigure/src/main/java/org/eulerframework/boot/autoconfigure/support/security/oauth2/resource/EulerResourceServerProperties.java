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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "euler.security.oauth2.resourceserver")
public class EulerResourceServerProperties {
    private String[] urlPatterns;
    private String[] ignoredUrlPatterns;

    /**
     * Whether these chains honour an existing HTTP session as a credential
     * alongside Bearer tokens. On, a session established by the default web
     * chain authenticates requests here and owes a CSRF token, while Bearer
     * callers stay CSRF-exempt. Off (the default), the chains refuse to
     * restore any session-derived SecurityContext and disable CSRF,
     * reducing to strict Bearer-only even when a session-creating chain
     * is present in the same deployment.
     */
    private boolean sessionAware = false;

    public String[] getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(String[] urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public String[] getIgnoredUrlPatterns() {
        return ignoredUrlPatterns;
    }

    public void setIgnoredUrlPatterns(String[] ignoredUrlPatterns) {
        this.ignoredUrlPatterns = ignoredUrlPatterns;
    }

    public boolean isSessionAware() {
        return sessionAware;
    }

    public void setSessionAware(boolean sessionAware) {
        this.sessionAware = sessionAware;
    }
}
