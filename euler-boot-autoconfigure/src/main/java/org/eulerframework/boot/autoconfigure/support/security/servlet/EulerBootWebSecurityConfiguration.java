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
import org.eulerframework.security.web.endpoint.*;
import org.eulerframework.security.web.endpoint.csrf.EulerCsrfTokenController;
import org.eulerframework.security.web.endpoint.csrf.EulerJsonCsrfTokenController;
import org.eulerframework.security.web.endpoint.csrf.EulerXmlCsrfTokenController;
import org.eulerframework.security.web.endpoint.user.DefaultEulerUserSecurityController;
import org.eulerframework.security.web.endpoint.user.EulerUserSecurityController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.util.Assert;

import java.util.Collections;
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
                        .requestMatchers("/captcha/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .anyRequest().authenticated())
                //.csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .formLogin(formLogin -> formLogin
                        .loginPage(eulerBootSecurityWebEndpointProperties.getUser().getLoginPage())
                        .loginProcessingUrl(eulerBootSecurityWebEndpointProperties.getUser().getLoginProcessingUrl()))
                .logout(logout -> logout
                        .logoutUrl(eulerBootSecurityWebEndpointProperties.getUser().getLogoutProcessingUrl()));
        return http.build();
    }

    private Map<String, String> hiddenInputs(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        return (token != null) ? Collections.singletonMap(token.getParameterName(), token.getToken())
                : Collections.emptyMap();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(EulerCsrfTokenController.class)
    static class EulerCsrfEndpointConfiguration {
        @Bean
        public EulerXmlCsrfTokenController eulerCsrfViewController() {
            return new EulerXmlCsrfTokenController();
        }

        @Bean
        public EulerJsonCsrfTokenController eulerCsrfAjaxController() {
            return new EulerJsonCsrfTokenController();
        }
    }


    @Bean
    @ConditionalOnMissingBean(EulerUserSecurityController.class)
    public DefaultEulerUserSecurityController eulerSecurityController() {
        return new DefaultEulerUserSecurityController();
    }

    @Bean
    @ConditionalOnMissingBean(DefaultEulerCaptchaController.class)
    public DefaultEulerCaptchaController eulerCaptchaController() {
        return new DefaultEulerCaptchaController();
    }
}
