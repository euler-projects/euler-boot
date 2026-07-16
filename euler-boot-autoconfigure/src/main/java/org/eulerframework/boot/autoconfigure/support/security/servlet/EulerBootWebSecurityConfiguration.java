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
package org.eulerframework.boot.autoconfigure.support.security.servlet;

import jakarta.servlet.http.HttpServletRequest;
import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityAppAttestProperties;
import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityOtpProperties;
import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.boot.autoconfigure.support.security.util.SecurityFilterUtils;
import org.eulerframework.security.config.annotation.web.configurers.appattest.AppAttestSecurityConfigurer;
import org.eulerframework.security.authentication.otp.OtpTestAccountSupport;
import org.eulerframework.security.config.annotation.web.configurers.oauth2.OAuth2LoginSecurityConfigurer;
import org.eulerframework.security.config.annotation.web.configurers.otp.OtpSecurityConfigurer;
import org.eulerframework.security.core.captcha.view.DefaultSmsCaptchaView;
import org.eulerframework.security.core.captcha.view.SmsCaptchaView;
import org.eulerframework.security.web.access.EulerAccessDeniedHandler;
import org.eulerframework.security.web.authentication.LoginPageAuthenticationEntryPoint;
import org.eulerframework.security.web.endpoint.*;
import org.eulerframework.security.web.endpoint.csrf.EulerSecurityCsrfTokenEndpoint;
import org.eulerframework.security.web.endpoint.csrf.EulerSecurityJsonCsrfTokenController;
import org.eulerframework.security.web.endpoint.csrf.EulerSecurityXmlCsrfTokenController;
import org.eulerframework.security.web.endpoint.password.EulerSecurityPasswordAjaxController;
import org.eulerframework.security.web.endpoint.password.EulerSecurityPasswordEndpoint;
import org.eulerframework.security.web.endpoint.password.EulerSecurityPasswordPageController;
import org.eulerframework.security.web.endpoint.signup.EulerSecuritySignupAjaxController;
import org.eulerframework.security.web.endpoint.signup.EulerSecuritySignupEndpoint;
import org.eulerframework.security.web.endpoint.signup.EulerSecuritySignupPageController;
import org.eulerframework.security.web.endpoint.user.EulerSecurityUserPageController;
import org.eulerframework.security.web.endpoint.user.EulerSecurityUserEndpoint;
import org.eulerframework.sms.ConsoleSmsSenderFactory;
import org.eulerframework.sms.SmsSenderFactory;
import org.eulerframework.web.core.base.controller.PageRender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "euler.security.web", name = "enabled", havingValue = "true")
public class EulerBootWebSecurityConfiguration {
    private final Logger logger = LoggerFactory.getLogger(EulerBootWebSecurityConfiguration.class);

    /**
     * Shared {@link CsrfTokenRepository} used by both the default web security
     * filter chain (form-login / session based) and the hybrid resource-server
     * filter chain that accepts session authentication alongside Bearer tokens.
     * Centralizing the bean keeps the CSRF cookie definition (name, attributes)
     * in a single place so the two chains remain interoperable for browser
     * clients sharing the same session.
     *
     * <p>This bean is gated by {@code euler.security.web.enabled=true} because
     * its hybrid use makes sense only when the default web security chain (which
     * actually performs the form login that populates the session) is active.
     * When web security is disabled, the resource-server side falls back to the
     * pure stateless Bearer chain which does not need this bean.</p>
     */
    @Bean
    @ConditionalOnMissingBean(CsrfTokenRepository.class)
    public CsrfTokenRepository csrfTokenRepository() {
        // The XSRF-TOKEN cookie is written by Spring Security itself, so it
        // is NOT covered by server.servlet.session.cookie.* (which only
        // governs the JSESSIONID cookie). The defaults below are inherited
        // from Spring Security 6 and are already aligned with the project's
        // baseline:
        //   * HttpOnly = true   (front-end retrieves the token via /_csrf)
        //   * SameSite = Lax
        //   * Secure   = request.isSecure()  -> emits Secure on production
        //                HTTPS once server.forward-headers-strategy=framework
        //                is honoured by the consuming application.
        // No further customisation is required; resist the temptation to
        // pin Secure to a hard-coded boolean here, which would break either
        // local http://localhost development (if true) or production HTTPS
        // hardening (if false).
        return new CookieCsrfTokenRepository();
    }

    @Bean(SecurityFilterChainBeanNames.DEFAULT_SECURITY_FILTER_CHAIN)
    @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.DEFAULT_SECURITY_FILTER_CHAIN)
    @Order(SecurityFilterProperties.BASIC_AUTH_ORDER)
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            @Qualifier(SecurityFilterChainBeanNames.LOGIN_PAGE_AUTHENTICATION_ENTRY_POINT)
            LoginPageAuthenticationEntryPoint loginPageEntryPoint,
            CsrfTokenRepository csrfTokenRepository,
            EulerBootSecurityWebProperties eulerBootSecurityWebProperties,
            EulerBootSecurityWebAuthnProperties eulerBootSecurityWebAuthnProperties,
            EulerBootSecurityWebEndpointProperties eulerBootSecurityWebEndpointProperties,
            EulerBootSecurityAppAttestProperties eulerBootSecurityAppAttestProperties,
            EulerBootSecurityOtpProperties eulerBootSecurityOtpProperties,
            ObjectProvider<WebAuthnPresent> wenAuthnPresent,
            ObjectProvider<DeviceAttestPresent> deviceAttestPresent,
            ObjectProvider<OAuth2LoginPresent> oauth2LoginPresent) throws Exception {
        Assert.isTrue(eulerBootSecurityWebProperties.isEnabled(), "euler web properties disabled, can not init defaultSecurityFilterChain");
        this.logger.debug("Create default security filter chain");

        String[] urlPatterns = eulerBootSecurityWebProperties.getUrlPatterns();
        String[] ignoredUrlPatterns = eulerBootSecurityWebProperties.getIgnoredUrlPatterns();
        SecurityFilterUtils.configSecurityMatcher(http, urlPatterns, ignoredUrlPatterns);
//        DefaultLogoutPageGeneratingFilter defaultLogoutPageGeneratingFilter = new DefaultLogoutPageGeneratingFilter();
//        defaultLogoutPageGeneratingFilter.setResolveHiddenInputs(this::hiddenInputs);

        SimpleUrlAuthenticationSuccessHandler successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter(eulerBootSecurityWebEndpointProperties.getUser().getLoginSuccessRedirectParameter());

        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(eulerBootSecurityWebEndpointProperties.getSignup().getSignupPage()).permitAll()
                        .requestMatchers(eulerBootSecurityWebEndpointProperties.getSignup().getSignupProcessingUrl()).permitAll()
                        .requestMatchers(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage()).permitAll()
                        .requestMatchers("/assets/**").permitAll()
                        .requestMatchers("/captcha").permitAll()
                        .requestMatchers("/captcha/validCaptcha").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .requestCache(RequestCacheConfigurer::disable)
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .formLogin(formLogin -> formLogin
                        .loginPage(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage())
                        .loginProcessingUrl(eulerBootSecurityWebEndpointProperties.getUser().getLoginProcessingUrl())
                        .successHandler(successHandler)
                        .addObjectPostProcessor(new ObjectPostProcessor<AuthenticationEntryPoint>() {
                            @Override
                            @SuppressWarnings("unchecked")
                            public AuthenticationEntryPoint postProcess(AuthenticationEntryPoint object) {
                                if (LoginUrlAuthenticationEntryPoint.class.isAssignableFrom(object.getClass())) {
                                    logger.info("Default LoginUrlAuthenticationEntryPoint was replaced with {} bean.",
                                            SecurityFilterChainBeanNames.LOGIN_PAGE_AUTHENTICATION_ENTRY_POINT);
                                    return loginPageEntryPoint;
                                } else {
                                    return object;
                                }
                            }
                        }))
                .logout(logout -> logout
                        .logoutUrl(eulerBootSecurityWebEndpointProperties.getUser().getLogoutProcessingUrl()));

        if (eulerBootSecurityWebAuthnProperties.isEnabled()) {
            if (wenAuthnPresent.getIfAvailable() == null) {
                throw new IllegalStateException("WebAuthn is enabled but the required dependency is missing. " +
                        "Please add the org.eulerframework:euler-security-wenauthn dependency to your project.");
            }
            logger.debug("WebAuthn dependency detected and enabled, configuring WebAuthn support.");
            http
                    .webAuthn((webAuthn) -> webAuthn
                            .rpId(eulerBootSecurityWebAuthnProperties.getRpId())
                            .allowedOrigins(eulerBootSecurityWebAuthnProperties.getAllowedOrigins())
                    );
        }

        if (eulerBootSecurityAppAttestProperties.isEnabled()) {
            if (deviceAttestPresent.getIfAvailable() == null) {
                throw new IllegalStateException("App Attest is enabled but the required dependency is missing. " +
                        "Please add the com.webauthn4j:webauthn4j-appattest dependency to your project.");
            }
            logger.debug("App Attest dependency detected and enabled, configuring App Attest registration endpoints.");
            http.with(new AppAttestSecurityConfigurer(), Customizer.withDefaults());
        }

        if (eulerBootSecurityOtpProperties.isEnabled()) {
            logger.debug("OTP module enabled, configuring OTP ticket issue endpoint.");
            OtpTestAccountSupport otpTestAccountSupport = null;
            EulerBootSecurityOtpProperties.Test test = eulerBootSecurityOtpProperties.getTest();
            if (test != null && test.isUsable()) {
                otpTestAccountSupport = new OtpTestAccountSupport(test.getAccounts(), test.getFixedOtp());
                logger.warn("OTP test-account short-circuit is ENABLED ({} account(s)) - DO NOT use in production.",
                        otpTestAccountSupport.getAccounts().size());
            }
            OtpTestAccountSupport otpTestAccountSupportFinal = otpTestAccountSupport;
            http.with(new OtpSecurityConfigurer(), otp -> otp
                    .issueEndpointUri(eulerBootSecurityOtpProperties.getIssueEndpointUri())
                    .pkceRequired(eulerBootSecurityOtpProperties.getPkce().isEnabled())
                    .testAccountSupport(otpTestAccountSupportFinal));
        }

        boolean anyOAuth2LoginMethod = eulerBootSecurityWebProperties.getLoginMethods().values().stream()
                .anyMatch(m -> m != null && "oauth2".equals(m.getType()));
        if (anyOAuth2LoginMethod) {
            if (oauth2LoginPresent.getIfAvailable() == null) {
                throw new IllegalStateException("At least one entry under " +
                        "euler.security.web.login-methods.* declares type=oauth2 " +
                        "but the required dependencies are missing. Please add the " +
                        "org.eulerframework:euler-security-oauth2-client dependency and " +
                        "org.springframework.boot:spring-boot-starter-oauth2-client to your project.");
            }
            logger.debug("At least one type=oauth2 login-method declared; configuring oauth2Login() branch.");
            http.with(new OAuth2LoginSecurityConfigurer(), oauth2 -> oauth2
                    .loginPage(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage())
                    .targetUrlParameter(
                            eulerBootSecurityWebEndpointProperties.getUser().getLoginSuccessRedirectParameter()));
        }

        this.configAccessDeniedHandler(http);
        return http.build();
    }

    private <H extends HttpSecurityBuilder<H>> void configAccessDeniedHandler(H http) {
//        LinkedHashMap<Class<? extends AccessDeniedException>, AccessDeniedHandler> handlers = new LinkedHashMap<>();
//        handlers.put(InvalidCsrfTokenException.class, new EulerAccessDeniedHandler());
        AccessDeniedHandlerImpl defaultAccessDeniedHandler = new EulerAccessDeniedHandler();
        // custom accessDeniedPage: defaultAccessDeniedHandler.setErrorPage("accessDeniedPage");
//        DelegatingAccessDeniedHandler delegatingAccessDeniedHandler = new DelegatingAccessDeniedHandler(handlers, defaultAccessDeniedHandler);

        @SuppressWarnings("unchecked")
        ExceptionHandlingConfigurer<H> exceptionConfig = http.getConfigurer(ExceptionHandlingConfigurer.class);
        exceptionConfig.accessDeniedHandler(defaultAccessDeniedHandler);
    }

    private Map<String, String> hiddenInputs(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return (token != null) ? Collections.singletonMap(token.getParameterName(), token.getToken())
                : Collections.emptyMap();
    }

    @Bean(SecurityFilterChainBeanNames.LOGIN_PAGE_AUTHENTICATION_ENTRY_POINT)
    @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.LOGIN_PAGE_AUTHENTICATION_ENTRY_POINT)
    public LoginPageAuthenticationEntryPoint loginPageAuthenticationEntryPoint(
            EulerBootSecurityWebEndpointProperties eulerBootSecurityWebEndpointProperties) {
        LoginPageAuthenticationEntryPoint loginUrlAuthenticationEntryPoint = new LoginPageAuthenticationEntryPoint();
        loginUrlAuthenticationEntryPoint.setLoginPage(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage());
        loginUrlAuthenticationEntryPoint.setRedirectParameter(eulerBootSecurityWebEndpointProperties.getUser().getLoginSuccessRedirectParameter());
        return loginUrlAuthenticationEntryPoint;
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(EulerSecurityCsrfTokenEndpoint.class)
    @ConditionalOnProperty(
            prefix = EulerSecurityEndpoints.CSRF_ENDPOINT_PROP_PREFIX,
            name = EulerSecurityEndpoints.ENABLED_PROP,
            havingValue = "true",
            matchIfMissing = EulerSecurityEndpoints.CSRF_ENABLED)
    static class EulerSecurityCsrfTokenEndpointConfiguration {
        @Bean
        public EulerSecurityXmlCsrfTokenController eulerSecurityXmlCsrfTokenController(PageRender pageRender) {
            return new EulerSecurityXmlCsrfTokenController(pageRender);
        }

        @Bean
        public EulerSecurityJsonCsrfTokenController eulerSecurityJsonCsrfTokenController() {
            return new EulerSecurityJsonCsrfTokenController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(EulerSecurityUserEndpoint.class)
    @ConditionalOnProperty(
            prefix = EulerSecurityEndpoints.USER_ENDPOINT_PROP_PREFIX,
            name = EulerSecurityEndpoints.ENABLED_PROP,
            havingValue = "true",
            matchIfMissing = EulerSecurityEndpoints.USER_ENABLED)
    static class EulerSecurityUserEndpointConfiguration {
        @Bean
        public EulerSecurityUserPageController eulerSecurityUserPageController(PageRender pageRender) {
            return new EulerSecurityUserPageController(pageRender);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(EulerSecuritySignupEndpoint.class)
    @ConditionalOnProperty(
            prefix = EulerSecurityEndpoints.SIGNUP_ENDPOINT_PROP_PREFIX,
            name = EulerSecurityEndpoints.ENABLED_PROP,
            havingValue = "true",
            matchIfMissing = EulerSecurityEndpoints.SIGNUP_ENABLED)
    static class EulerSecuritySignupEndpointConfiguration {
        @Bean
        public EulerSecuritySignupPageController eulerSecuritySignupPageController(PageRender pageRender) {
            return new EulerSecuritySignupPageController(pageRender);
        }

        @Bean
        public EulerSecuritySignupAjaxController eulerSecuritySignupAjaxController() {
            return new EulerSecuritySignupAjaxController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(EulerSecurityPasswordEndpoint.class)
    @ConditionalOnProperty(
            prefix = EulerSecurityEndpoints.PASSWORD_ENDPOINT_PROP_PREFIX,
            name = EulerSecurityEndpoints.ENABLED_PROP,
            havingValue = "true",
            matchIfMissing = EulerSecurityEndpoints.PASSWORD_ENABLED)
    static class EulerSecurityPasswordEndpointConfiguration {
        @Bean
        public EulerSecurityPasswordPageController eulerSecurityPasswordPageController(PageRender pageRender) {
            return new EulerSecurityPasswordPageController(pageRender);
        }

        @Bean
        public EulerSecurityPasswordAjaxController eulerSecurityPasswordAjaxController() {
            return new EulerSecurityPasswordAjaxController();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class EulerSecurityCaptchaEndpointConfiguration {
        @Bean
        @ConditionalOnClass(ConsoleSmsSenderFactory.class)
        @ConditionalOnMissingBean(SmsSenderFactory.class)
        public SmsSenderFactory smsSenderFactory() {
            return new ConsoleSmsSenderFactory();
        }

        @Bean
        @ConditionalOnBean(SmsSenderFactory.class)
        public SmsCaptchaView smsCaptchaView(SmsSenderFactory smsSenderFactory) {
            return new DefaultSmsCaptchaView(smsSenderFactory);
        }

        @Bean
        @ConditionalOnMissingBean(DefaultEulerCaptchaController.class)
        public DefaultEulerCaptchaController eulerCaptchaController(@Autowired(required = false) SmsCaptchaView smsCaptchaView) {
            DefaultEulerCaptchaController defaultEulerCaptchaController = new DefaultEulerCaptchaController();

            if (smsCaptchaView != null) {
                defaultEulerCaptchaController.setSmsCaptchaView(smsCaptchaView);
            }

            return defaultEulerCaptchaController;
        }
    }

    @Component
    @ConditionalOnClass(WebAuthnAuthenticationFilter.class)
    public static class WebAuthnPresent {
    }

    @Component
    @ConditionalOnClass(name = "com.webauthn4j.appattest.DeviceCheckManager")
    public static class DeviceAttestPresent {
    }

    @Component
    @ConditionalOnClass(name = {
            "org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter",
            "org.eulerframework.security.oauth2.client.authentication.OAuth2LoginPrincipalPromotingSuccessHandler"
    })
    public static class OAuth2LoginPresent {
    }
}
