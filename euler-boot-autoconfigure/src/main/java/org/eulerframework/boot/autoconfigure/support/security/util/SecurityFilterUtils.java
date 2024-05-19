package org.eulerframework.boot.autoconfigure.support.security.util;

import org.apache.commons.lang3.ArrayUtils;
import org.eulerframework.security.web.util.matcher.RequestMatcherCreator;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class SecurityFilterUtils {
    public static void configSecurityMatcher(HttpSecurity http, String[] urlPatterns, String[] ignoredUrlPatterns) {
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
    }
}
