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
package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.web.endpoint.EulerSecurityEndpoints;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "euler.security.web")
public class EulerBootSecurityWebProperties {
    private String[] urlPatterns;
    private String[] ignoredUrlPatterns;
    private boolean enabled = false;
    private Signup signup = new Signup();
    private String signupPage = EulerSecurityEndpoints.SIGNUP_PAGE;
    private String loginPage = EulerSecurityEndpoints.LOGIN_PAGE;
    private String logoutPage = EulerSecurityEndpoints.LOGOUT_PAGE;
    private String changePasswordPage = EulerSecurityEndpoints.CHANGE_PASSWORD_PAGE;
    private String loginProcessingUrl = EulerSecurityEndpoints.LOGIN_PROCESSING_URL;
    private String logoutProcessingUrl = EulerSecurityEndpoints.LOGOUT_PROCESSING_URL;
    private String changePasswordProcessingUrl = EulerSecurityEndpoints.CHANGE_PASSWORD_PROCESSING_URL;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Signup getSignup() {
        return signup;
    }

    public void setSignup(Signup signup) {
        this.signup = signup;
    }

    public String getSignupPage() {
        return signupPage;
    }

    public void setSignupPage(String signupPage) {
        this.signupPage = signupPage;
    }

    public String getLoginPage() {
        return loginPage;
    }

    public void setLoginPage(String loginPage) {
        this.loginPage = loginPage;
    }

    public String getLogoutPage() {
        return logoutPage;
    }

    public void setLogoutPage(String logoutPage) {
        this.logoutPage = logoutPage;
    }

    public String getChangePasswordPage() {
        return changePasswordPage;
    }

    public void setChangePasswordPage(String changePasswordPage) {
        this.changePasswordPage = changePasswordPage;
    }

    public String getLoginProcessingUrl() {
        return loginProcessingUrl;
    }

    public void setLoginProcessingUrl(String loginProcessingUrl) {
        this.loginProcessingUrl = loginProcessingUrl;
    }

    public String getLogoutProcessingUrl() {
        return logoutProcessingUrl;
    }

    public void setLogoutProcessingUrl(String logoutProcessingUrl) {
        this.logoutProcessingUrl = logoutProcessingUrl;
    }

    public String getChangePasswordProcessingUrl() {
        return changePasswordProcessingUrl;
    }

    public void setChangePasswordProcessingUrl(String changePasswordProcessingUrl) {
        this.changePasswordProcessingUrl = changePasswordProcessingUrl;
    }

    public static class Signup {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
