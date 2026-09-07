/*
 * Copyright 2013-2026 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = EulerSecurityEndpoints.PROPERTY_NAME_PREFIX)
public class EulerBootSecurityWebEndpointProperties {

    /**
     * CSRF token endpoint settings.
     */
    private Csrf csrf = new Csrf();

    /**
     * Login and logout endpoint settings.
     */
    private User user = new User();

    /**
     * Change-password endpoint settings.
     */
    private Password password = new Password();

    /**
     * Self-service signup endpoint settings.
     */
    private Signup signup = new Signup();

    /**
     * Login-method endpoint family settings: the unified dispatch entry
     * point and the discovery endpoint serving the offered methods.
     */
    private LoginMethods loginMethods = new LoginMethods();

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

    public Password getPassword() {
        return password;
    }

    public void setPassword(Password password) {
        this.password = password;
    }

    public Signup getSignup() {
        return signup;
    }

    public void setSignup(Signup signup) {
        this.signup = signup;
    }

    public LoginMethods getLoginMethods() {
        return loginMethods;
    }

    public void setLoginMethods(LoginMethods loginMethods) {
        this.loginMethods = loginMethods;
    }

    public static class LoginMethods {

        /**
         * Unified dispatch entry point settings.
         */
        private DispatchEndpoint dispatch = new DispatchEndpoint();

        /**
         * Discovery endpoint settings.
         */
        private DiscoveryEndpoint discovery = new DiscoveryEndpoint();

        public DispatchEndpoint getDispatch() {
            return dispatch;
        }

        public void setDispatch(DispatchEndpoint dispatch) {
            this.dispatch = dispatch;
        }

        public DiscoveryEndpoint getDiscovery() {
            return discovery;
        }

        public void setDiscovery(DiscoveryEndpoint discovery) {
            this.discovery = discovery;
        }

        /**
         * Configuration of the unified login-method dispatch endpoint,
         * which routes a submission to the declared login method selected
         * by the method parameter.
         *
         * <p>The dispatch endpoint is a backend convenience serving the
         * bundled reference login page; it is not a mandatory contract for
         * frontends. Deployments that route all login methods on their own
         * (e.g. an SPA submitting via XHR to native endpoints) may disable
         * it entirely to keep the filter out of the security chain.
         */
        public static class DispatchEndpoint {

            /**
             * Whether to register the login-method dispatch filter. When
             * false the filter is not added to the security chain at all.
             */
            private boolean enabled = EulerSecurityEndpoints.LOGIN_METHODS_DISPATCH_ENABLED;

            /**
             * Path receiving login-method submissions (POST). Independent
             * of the login page and the form-login processing URL, even
             * though all three default to the same value.
             */
            private String processingUrl = EulerSecurityEndpoints.LOGIN_METHODS_DISPATCH_PROCESSING_URL;

            /**
             * Request parameter selecting the login method; its value is a
             * key declared under euler.security.login-method.
             */
            private String methodParameter = EulerSecurityEndpoints.LOGIN_METHODS_DISPATCH_METHOD_PARAMETER;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getProcessingUrl() {
                return processingUrl;
            }

            public void setProcessingUrl(String processingUrl) {
                this.processingUrl = processingUrl;
            }

            public String getMethodParameter() {
                return methodParameter;
            }

            public void setMethodParameter(String methodParameter) {
                this.methodParameter = methodParameter;
            }
        }

        /**
         * Configuration of the login-method discovery endpoint serving the
         * offered methods as a JSON array (GET), read by standalone SPA
         * deployments that cannot receive the list through template
         * injection. Anonymous, snake-case envelope, mirroring the built-in
         * authentication-service endpoints.
         */
        public static class DiscoveryEndpoint {

            /**
             * Whether to register the login-method discovery filter. When
             * false the filter is not added to the security chain at all.
             */
            private boolean enabled = EulerSecurityEndpoints.LOGIN_METHODS_DISCOVERY_ENABLED;

            /**
             * URL serving the offered login methods (GET).
             */
            private String fetchingUrl = EulerSecurityEndpoints.LOGIN_METHODS_DISCOVERY_FETCHING_URL;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getFetchingUrl() {
                return fetchingUrl;
            }

            public void setFetchingUrl(String fetchingUrl) {
                this.fetchingUrl = fetchingUrl;
            }
        }
    }

    public static class Csrf {

        /**
         * Whether to expose the CSRF token endpoint, which lets
         * browser clients fetch the current token before issuing
         * state-changing requests.
         */
        private boolean enabled = EulerSecurityEndpoints.CSRF_ENABLED;

        /**
         * Fetching URL of the CSRF token endpoint.
         */
        private String fetchingUrl = EulerSecurityEndpoints.CSRF_FETCHING_URL;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFetchingUrl() {
            return fetchingUrl;
        }

        public void setFetchingUrl(String fetchingUrl) {
            this.fetchingUrl = fetchingUrl;
        }
    }

    public static class User {

        /**
         * Whether to expose the built-in login and logout pages.
         */
        private boolean enabled = EulerSecurityEndpoints.USER_ENABLED;

        /**
         * Path serving the login page (GET).
         */
        private String loginPage = EulerSecurityEndpoints.USER_LOGIN_PAGE;

        /**
         * Path serving the logout confirmation page (GET).
         */
        private String logoutPage = EulerSecurityEndpoints.USER_LOGOUT_PAGE;

        /**
         * Path processing username and password credentials (POST),
         * handled by Spring Security's form-login filter. Independent
         * of the login page and the login-method dispatch endpoint,
         * even though all three default to the same value.
         */
        private String loginProcessingUrl = EulerSecurityEndpoints.USER_LOGIN_PROCESSING_URL;

        /**
         * Path processing logout requests (POST).
         */
        private String logoutProcessingUrl = EulerSecurityEndpoints.USER_LOGOUT_PROCESSING_URL;

        /**
         * Request parameter carrying the URL to redirect to after a
         * successful login.
         */
        private String loginSuccessRedirectParameter = EulerSecurityEndpoints.USER_LOGIN_SUCCESS_REDIRECT_PARAMETER;

        /**
         * The login methods the built-in login page offers, in the order
         * it offers them: the names clients address the methods by, that
         * is the declaration keys beneath
         * {@code euler.security.login-method.<method-type>} unless a
         * declaration overrides one with {@code method-name}.
         *
         * <p>A method left out is hidden from the built-in page while
         * staying dispatchable; naming one that is not declared, or one
         * twice, fails when the page renders rather than being silently
         * dropped. Empty, the default, offers everything the login method
         * service serves, in its own order.
         */
        private List<String> loginPageMethods = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

        public String getLoginProcessingUrl() {
            return loginProcessingUrl;
        }

        public void setLoginProcessingUrl(String loginProcessingUrl) {
            this.loginProcessingUrl = loginProcessingUrl;
        }

        public String getLoginSuccessRedirectParameter() {
            return loginSuccessRedirectParameter;
        }

        public void setLoginSuccessRedirectParameter(String loginSuccessRedirectParameter) {
            this.loginSuccessRedirectParameter = loginSuccessRedirectParameter;
        }

        public List<String> getLoginPageMethods() {
            return loginPageMethods;
        }

        public void setLoginPageMethods(List<String> loginPageMethods) {
            this.loginPageMethods = loginPageMethods;
        }

        public String getLogoutProcessingUrl() {
            return logoutProcessingUrl;
        }

        public void setLogoutProcessingUrl(String logoutProcessingUrl) {
            this.logoutProcessingUrl = logoutProcessingUrl;
        }
    }

    public static class Password {

        /**
         * Whether to expose the change-password page and endpoint.
         */
        private boolean enabled = EulerSecurityEndpoints.PASSWORD_ENABLED;

        /**
         * Path serving the change-password page (GET).
         */
        private String changePasswordPage = EulerSecurityEndpoints.PASSWORD_CHANGE_PASSWORD_PAGE;

        /**
         * Path processing change-password submissions (POST).
         */
        private String changePasswordProcessingUrl = EulerSecurityEndpoints.PASSWORD_CHANGE_PASSWORD_PROCESSING_URL;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getChangePasswordPage() {
            return changePasswordPage;
        }

        public void setChangePasswordPage(String changePasswordPage) {
            this.changePasswordPage = changePasswordPage;
        }

        public String getChangePasswordProcessingUrl() {
            return changePasswordProcessingUrl;
        }

        public void setChangePasswordProcessingUrl(String changePasswordProcessingUrl) {
            this.changePasswordProcessingUrl = changePasswordProcessingUrl;
        }
    }


    public static class Signup {

        /**
         * Whether to expose the self-service signup page and endpoint.
         */
        private boolean enabled = EulerSecurityEndpoints.SIGNUP_ENABLED;

        /**
         * Path serving the signup page (GET).
         */
        private String signupPage = EulerSecurityEndpoints.SIGNUP_PAGE;

        /**
         * Path processing signup submissions (POST).
         */
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
