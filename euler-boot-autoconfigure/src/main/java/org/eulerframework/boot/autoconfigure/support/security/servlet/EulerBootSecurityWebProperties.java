/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "euler.security.web")
public class EulerBootSecurityWebProperties {
    private String[] urlPatterns;
    private boolean enabled = false;
    private boolean enableEmailSignIn = false;
    private boolean enableMobileSignIn = false;
    private Duration userDetailsCacheExpireTime = Duration.ofMinutes(1);

    public String[] getUrlPatterns() {
        return urlPatterns;
    }

    public void setUrlPatterns(String[] urlPatterns) {
        this.urlPatterns = urlPatterns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnableEmailSignIn() {
        return enableEmailSignIn;
    }

    public void setEnableEmailSignIn(boolean enableEmailSignIn) {
        this.enableEmailSignIn = enableEmailSignIn;
    }

    public boolean isEnableMobileSignIn() {
        return enableMobileSignIn;
    }

    public void setEnableMobileSignIn(boolean enableMobileSignIn) {
        this.enableMobileSignIn = enableMobileSignIn;
    }

    public Duration getUserDetailsCacheExpireTime() {
        return userDetailsCacheExpireTime;
    }

    public void setUserDetailsCacheExpireTime(Duration userDetailsCacheExpireTime) {
        this.userDetailsCacheExpireTime = userDetailsCacheExpireTime;
    }
}
