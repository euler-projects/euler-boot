package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.apache.commons.lang3.ArrayUtils;
import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.eulerframework.security.web.util.matcher.RequestMatcherCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "euler.security.web", name = "enabled", havingValue = "true")
public class EulerBootWebSecurityConfiguration {
    private final Logger logger = LoggerFactory.getLogger(EulerBootWebSecurityConfiguration.class);

    @Bean(SecurityFilterChainBeanNames.DEFAULT_SECURITY_FILTER_CHAIN)
    @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.DEFAULT_SECURITY_FILTER_CHAIN)
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, EulerBootSecurityWebProperties eulerBootSecurityWebProperties) throws Exception {
        Assert.isTrue(eulerBootSecurityWebProperties.isEnabled(), "euler web properties disabled, can not init defaultSecurityFilterChain");
        this.logger.debug("Create default security filter chain");

        String[] urlPatterns = eulerBootSecurityWebProperties.getUrlPatterns();
        String[] ignoredUrlPatterns = eulerBootSecurityWebProperties.getIgnoredUrlPatterns();

        if (ArrayUtils.isNotEmpty(urlPatterns) && ArrayUtils.isNotEmpty(ignoredUrlPatterns)) {
            RequestMatcherCreator requestMatcherCreator = new RequestMatcherCreator(http.getSharedObject(ApplicationContext.class));
            RequestMatcher requestMatcher = requestMatcherCreator.securityMatcher(urlPatterns);
            RequestMatcher ignoredRequestMatcher = requestMatcherCreator.securityMatcher(ignoredUrlPatterns);
            http.securityMatcher(new AndRequestMatcher(requestMatcher, new NegatedRequestMatcher(ignoredRequestMatcher)));
        } else if (ArrayUtils.isNotEmpty(urlPatterns)) {
            http.securityMatcher(urlPatterns);
        } else if (ArrayUtils.isNotEmpty(ignoredUrlPatterns)) {
            RequestMatcherCreator requestMatcherCreator = new RequestMatcherCreator(http.getSharedObject(ApplicationContext.class));
            RequestMatcher ignoredRequestMatcher = requestMatcherCreator.securityMatcher(ignoredUrlPatterns);
            http.securityMatcher(new NegatedRequestMatcher(ignoredRequestMatcher));
        }

        http
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .formLogin(withDefaults());
        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        return new UsernamePasswordAuthenticationUserContext();
    }
}
