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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Pins the credential discrimination of
 * {@link EulerResourceServerSecurityConfiguration.SessionAwareCsrfProtectionMatcher}:
 * CSRF is owed by session-borne callers only, never by safe methods,
 * anonymous callers or Bearer callers.
 */
public class SessionAwareCsrfProtectionMatcherTests {

    private final EulerResourceServerSecurityConfiguration.SessionAwareCsrfProtectionMatcher matcher =
            new EulerResourceServerSecurityConfiguration.SessionAwareCsrfProtectionMatcher();

    @After
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn() {
        Authentication session = UsernamePasswordAuthenticationToken.authenticated(
                "user", null, AuthorityUtils.NO_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(session);
    }

    private static void signInAnonymously() {
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
    }

    /**
     * Stubs just the three accessors the matcher reads; everything else is
     * out of contract and fails loudly if ever touched.
     */
    private static HttpServletRequest request(String method, String authorization, String accessTokenParameter) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                SessionAwareCsrfProtectionMatcherTests.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, invoked, args) -> switch (invoked.getName()) {
                    case "getMethod" -> method;
                    case "getHeader" -> HttpHeaders.AUTHORIZATION.equals(args[0]) ? authorization : null;
                    case "getParameter" -> "access_token".equals(args[0]) ? accessTokenParameter : null;
                    default -> throw new UnsupportedOperationException(invoked.getName());
                });
    }

    @Test
    public void safeMethodWithSessionOwesNoCsrf() {
        signIn();
        assertFalse(matcher.matches(request("GET", null, null)));
    }

    @Test
    public void anonymousPostOwesNoCsrf() {
        assertFalse(matcher.matches(request("POST", null, null)));
    }

    @Test
    public void anonymousTokenPostOwesNoCsrf() {
        signInAnonymously();
        assertFalse(matcher.matches(request("POST", null, null)));
    }

    @Test
    public void sessionPostOwesCsrf() {
        signIn();
        assertTrue(matcher.matches(request("POST", null, null)));
    }

    @Test
    public void bearerHeaderExemptsASessionPost() {
        signIn();
        assertFalse(matcher.matches(request("POST", "Bearer some-token", null)));
    }

    @Test
    public void lowercaseBearerHeaderExemptsASessionPost() {
        signIn();
        assertFalse(matcher.matches(request("POST", "bearer some-token", null)));
    }

    @Test
    public void bearerQueryParameterExemptsASessionPost() {
        signIn();
        assertFalse(matcher.matches(request("POST", null, "some-token")));
    }

    @Test
    public void bearerOnlyPostOwesNoCsrf() {
        assertFalse(matcher.matches(request("POST", "Bearer some-token", null)));
    }

    @Test
    public void unrelatedAuthorizationHeaderDoesNotExemptASessionPost() {
        signIn();
        assertTrue(matcher.matches(request("POST", "Basic dXNlcjpwYXNz", null)));
    }
}
