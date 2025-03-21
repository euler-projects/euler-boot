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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "euler.security.oauth2.authorizationserver")
public class EulerBootAuthorizationServerProperties {
    private TokenStoreType authorizationStoreType = TokenStoreType.IN_MEMORY;
    private Duration authorizationLifetime = Duration.ofDays(7);
    private String redisKeyPrefix = "euler:oauth2:auth";

    private String consentPage;

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
}
