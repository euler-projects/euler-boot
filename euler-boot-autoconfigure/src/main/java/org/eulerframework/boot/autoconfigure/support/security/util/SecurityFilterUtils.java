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
package org.eulerframework.boot.autoconfigure.support.security.util;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

public class SecurityFilterUtils {
    public static void configSecurityMatcher(HttpSecurity http, String[] urlPatterns, String[] ignoredUrlPatterns) {
        if (ArrayUtils.isNotEmpty(urlPatterns) && ArrayUtils.isNotEmpty(ignoredUrlPatterns)) {
            RequestMatcher requestMatcher = createRequestMatcher(http, urlPatterns);
            RequestMatcher ignoredRequestMatcher = createRequestMatcher(http, ignoredUrlPatterns);
            http.securityMatcher(new AndRequestMatcher(requestMatcher, new NegatedRequestMatcher(ignoredRequestMatcher)));
        } else if (ArrayUtils.isNotEmpty(urlPatterns)) {
            http.securityMatcher(urlPatterns);
        } else if (ArrayUtils.isNotEmpty(ignoredUrlPatterns)) {
            RequestMatcher ignoredRequestMatcher = createRequestMatcher(http, ignoredUrlPatterns);
            http.securityMatcher(new NegatedRequestMatcher(ignoredRequestMatcher));
        }
    }

    private static RequestMatcher createRequestMatcher(HttpSecurity http, String[] patterns) {
        PathPatternRequestMatcher.Builder builder = http.getSharedObject(PathPatternRequestMatcher.Builder.class);

        List<RequestMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            matchers.add(builder.matcher(pattern));
        }
        return new OrRequestMatcher(matchers);
    }
}
