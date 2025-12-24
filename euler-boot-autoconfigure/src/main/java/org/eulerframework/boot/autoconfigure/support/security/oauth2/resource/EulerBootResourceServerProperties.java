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
public class EulerBootResourceServerProperties {
    private String[] urlPatterns;
    private String[] ignoredUrlPatterns;
    private WechatLogin wechatLogin;

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

    public WechatLogin getWechatLogin() {
        return wechatLogin;
    }

    public void setWechatLogin(WechatLogin wechatLogin) {
        this.wechatLogin = wechatLogin;
    }

    public static class WechatLogin {
        private boolean enabled;
        private boolean autoCreateUserIfNotExists;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isAutoCreateUserIfNotExists() {
            return autoCreateUserIfNotExists;
        }

        public void setAutoCreateUserIfNotExists(boolean autoCreateUserIfNotExists) {
            this.autoCreateUserIfNotExists = autoCreateUserIfNotExists;
        }
    }
}
