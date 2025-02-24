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

import jakarta.servlet.http.HttpServletRequest;
import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.boot.autoconfigure.support.security.util.SecurityFilterUtils;
import org.eulerframework.security.core.captcha.view.DefaultSmsCaptchaView;
import org.eulerframework.security.core.captcha.view.SmsCaptchaView;
import org.eulerframework.security.web.access.EulerAccessDeniedHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "euler.security.web", name = "enabled", havingValue = "true")
public class EulerBootWebSecurityConfiguration {
    private final Logger logger = LoggerFactory.getLogger(EulerBootWebSecurityConfiguration.class);

    @Bean(SecurityFilterChainBeanNames.DEFAULT_SECURITY_FILTER_CHAIN)
    @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.DEFAULT_SECURITY_FILTER_CHAIN)
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            EulerBootSecurityWebProperties eulerBootSecurityWebProperties,
            EulerBootSecurityWebEndpointProperties eulerBootSecurityWebEndpointProperties) throws Exception {
        Assert.isTrue(eulerBootSecurityWebProperties.isEnabled(), "euler web properties disabled, can not init defaultSecurityFilterChain");
        this.logger.debug("Create default security filter chain");

        String[] urlPatterns = eulerBootSecurityWebProperties.getUrlPatterns();
        String[] ignoredUrlPatterns = eulerBootSecurityWebProperties.getIgnoredUrlPatterns();
        SecurityFilterUtils.configSecurityMatcher(http, urlPatterns, ignoredUrlPatterns);
//        DefaultLogoutPageGeneratingFilter defaultLogoutPageGeneratingFilter = new DefaultLogoutPageGeneratingFilter();
//        defaultLogoutPageGeneratingFilter.setResolveHiddenInputs(this::hiddenInputs);
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(eulerBootSecurityWebEndpointProperties.getSignup().getSignupPage()).permitAll()
                        .requestMatchers(eulerBootSecurityWebEndpointProperties.getSignup().getSignupProcessingUrl()).permitAll()
                        .requestMatchers(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage()).permitAll()
                        .requestMatchers("/captcha").permitAll()
                        .requestMatchers("/captcha/validCaptcha").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.csrfTokenRepository(new CookieCsrfTokenRepository()))
                .formLogin(formLogin -> formLogin
                        .loginPage(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage())
                        .loginProcessingUrl(eulerBootSecurityWebEndpointProperties.getUser().getLoginProcessingUrl()))
                .logout(logout -> logout
                        .logoutUrl(eulerBootSecurityWebEndpointProperties.getUser().getLogoutProcessingUrl()));

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

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(EulerSecurityCsrfTokenEndpoint.class)
    @ConditionalOnProperty(
            prefix = EulerSecurityEndpoints.CSRF_ENDPOINT_PROP_PREFIX,
            name = EulerSecurityEndpoints.ENABLED_PROP,
            havingValue = "true",
            matchIfMissing = EulerSecurityEndpoints.CSRF_ENABLED)
    static class EulerSecurityCsrfTokenEndpointConfiguration {
        @Bean
        public EulerSecurityXmlCsrfTokenController eulerSecurityXmlCsrfTokenController() {
            return new EulerSecurityXmlCsrfTokenController();
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
        public EulerSecurityUserPageController eulerSecurityUserPageController() {
            return new EulerSecurityUserPageController();
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
        public EulerSecuritySignupPageController eulerSecuritySignupPageController() {
            return new EulerSecuritySignupPageController();
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
        public EulerSecurityPasswordPageController eulerSecurityPasswordPageController() {
            return new EulerSecurityPasswordPageController();
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
}
