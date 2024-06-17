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

@ConfigurationProperties(prefix = "euler.security.web.endpoint")
public class EulerBootSecurityWebEndpointProperties {
    private Csrf csrf = new Csrf();
    private User user = new User();
    private Signup signup = new Signup();

    public Csrf getCsrf() {
        return csrf;
    }

    public void setCsrf(Csrf csrf) {
        this.csrf = csrf;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Signup getSignup() {
        return signup;
    }

    public void setSignup(Signup signup) {
        this.signup = signup;
    }

    public static class Csrf {
        private boolean enabled = EulerSecurityEndpoints.CSRF_ENABLED;
        private String path = EulerSecurityEndpoints.CSRF_PATH;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class User {
        private String loginPage = EulerSecurityEndpoints.USER_LOGIN_PAGE;
        private String logoutPage = EulerSecurityEndpoints.USER_LOGOUT_PAGE;
        private String changePasswordPage = EulerSecurityEndpoints.USER_CHANGE_PASSWORD_PAGE;
        private String loginProcessingUrl = EulerSecurityEndpoints.USER_LOGIN_PROCESSING_URL;
        private String logoutProcessingUrl = EulerSecurityEndpoints.USER_LOGOUT_PROCESSING_URL;
        private String changePasswordProcessingUrl = EulerSecurityEndpoints.USER_CHANGE_PASSWORD_PROCESSING_URL;

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



    public static class Signup {
        private boolean enabled = EulerSecurityEndpoints.SIGNUP_ENABLED;
        private String signupPage = EulerSecurityEndpoints.SIGNUP_PAGE;
        private String signupProcessingUrl = EulerSecurityEndpoints.SIGNUP_PROCESSING_URL;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSignupPage() {
            return signupPage;
        }

        public void setSignupPage(String signupPage) {
            this.signupPage = signupPage;
        }

        public String getSignupProcessingUrl() {
            return signupProcessingUrl;
        }

        public void setSignupProcessingUrl(String signupProcessingUrl) {
            this.signupProcessingUrl = signupProcessingUrl;
        }
    }
}
