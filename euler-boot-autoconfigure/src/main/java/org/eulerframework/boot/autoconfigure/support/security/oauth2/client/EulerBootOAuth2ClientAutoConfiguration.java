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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.client;

import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebProperties;
import org.eulerframework.common.util.collections.MapUtils;
import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.oauth2.client.authentication.OAuth2LoginPrincipalPromotingSuccessHandler;
import org.eulerframework.security.oauth2.client.authentication.PerRegistrationLoginPolicy;
import org.eulerframework.security.oauth2.client.web.OAuth2LoginMethodTypeHandler;
import org.eulerframework.security.web.endpoint.user.login.LoginMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Autoconfiguration for OAuth2 client-side beans backing the unified
 * {@code euler.security.web.login-methods.<name>.type: oauth2}
 * declaration:
 *
 * <ul>
 *   <li>{@link OAuth2LoginMethodTypeHandler} &mdash; turns each
 *       {@code type: oauth2} config entry into a
 *       {@code LoginMethodView} by resolving the referenced
 *       {@code spring.security.oauth2.client.registration.<key>} entry.</li>
 *   <li>{@link OAuth2LoginPrincipalPromotingSuccessHandler} &mdash; the
 *       success handler that promotes the freshly authenticated OIDC
 *       principal into a local {@code EulerUserDetails}, driven by a
 *       per-registration policy assembled from the same
 *       {@code login-methods} entries.</li>
 * </ul>
 *
 * <p>Ordered after Spring Boot's own {@code OAuth2ClientAutoConfiguration}
 * so that {@link ClientRegistrationRepository} (if any) is already in
 * the context by the time the {@link ConditionalOnBean} check is
 * evaluated.
 *
 * <p>The generic {@code LoginMethodContributor} dispatcher that iterates
 * {@code login-methods} entries and delegates to
 * {@link org.eulerframework.security.web.endpoint.user.login.LoginMethodTypeHandler}s
 * is registered separately in
 * {@code EulerBootSecurityWebAutoConfiguration} - login-method
 * dispatching is a servlet-web concern, this class is OAuth2-only.
 */
@AutoConfiguration(
        before = {
                EulerBootSecurityWebAutoConfiguration.class
        },
        after = {
                // Wait for Spring Boot to publish the default
                // ClientRegistrationRepository so this class's
                // @ConditionalOnBean check below can see it.
                OAuth2ClientAutoConfiguration.class
        }
)
@ConditionalOnClass(OAuth2LoginAuthenticationFilter.class)
@ConditionalOnBean(ClientRegistrationRepository.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EulerBootOAuth2ClientAutoConfiguration {

    private static final Logger logger =
            LoggerFactory.getLogger(EulerBootOAuth2ClientAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(OAuth2LoginMethodTypeHandler.class)
    public OAuth2LoginMethodTypeHandler oauth2LoginMethodTypeHandler(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new OAuth2LoginMethodTypeHandler(clientRegistrationRepository);
    }

    /**
     * Success handler that consumes the per-registration login policy
     * assembled from
     * {@code euler.security.web.login-methods.*} entries whose
     * {@code type == oauth2}.
     *
     * <p>Registrations declared under
     * {@code spring.security.oauth2.client.registration.*} but not
     * mentioned in {@code login-methods} receive no policy entry; the
     * success handler falls back to {@code autoCreateUser=false} with
     * {@code identityType=registrationId}, so a stray non-login
     * registration accessed via {@code /oauth2/authorization/...} will
     * only sign in already-known users.
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2LoginPrincipalPromotingSuccessHandler.class)
    public OAuth2LoginPrincipalPromotingSuccessHandler oauth2LoginPrincipalPromotingSuccessHandler(
            EulerUserService userService,
            UserIdentityService userIdentityService,
            EulerBootSecurityWebProperties webProperties) {
        OAuth2LoginPrincipalPromotingSuccessHandler handler =
                new OAuth2LoginPrincipalPromotingSuccessHandler(userService, userIdentityService);
        handler.setPoliciesByRegistrationId(buildPolicies(webProperties.getLoginMethods()));
        return handler;
    }

    /**
     * Translate every {@code type=oauth2} entry in
     * {@code login-methods} into a
     * {@code registrationId -> PerRegistrationLoginPolicy} mapping.
     * The registration ID resolves as
     * {@code properties.oauth-client-registration-id} defaulting to
     * the login-method key; identity-type defaults to the same value.
     */
    private static Map<String, PerRegistrationLoginPolicy> buildPolicies(
            Map<String, LoginMethod> loginMethods) {
        if (loginMethods == null || loginMethods.isEmpty()) {
            return Map.of();
        }
        Map<String, PerRegistrationLoginPolicy> policies = new LinkedHashMap<>();
        for (Map.Entry<String, LoginMethod> entry : loginMethods.entrySet()) {
            String name = entry.getKey();
            LoginMethod method = entry.getValue();
            if (method == null || !OAuth2LoginMethodTypeHandler.TYPE.equals(method.getType())) {
                continue;
            }
            Map<String, Object> properties = method.getProperties();
            String registrationId = MapUtils.getString(properties,
                    OAuth2LoginMethodTypeHandler.PROP_OAUTH_CLIENT_REGISTRATION_ID);
            if (registrationId == null || registrationId.isEmpty()) {
                registrationId = name;
            }
            String identityType = MapUtils.getString(properties, "identity-type");
            if (identityType == null || identityType.isEmpty()) {
                identityType = name;
            }
            boolean autoCreateUser = MapUtils.getBoolean(properties, "auto-create-user", false);
            List<String> defaultAuthorities = asStringList(properties, "default-authorities");
            if (autoCreateUser && defaultAuthorities.isEmpty()) {
                // Refuse to auto-create with an empty authority list:
                // an unauthenticated ghost row would only cause
                // harder-to-diagnose failures later.
                logger.warn("Login method '{}' has auto-create-user=true but no default-authorities; "
                        + "auto-creation is disabled for this registration.", name);
                autoCreateUser = false;
            }
            policies.put(registrationId,
                    new PerRegistrationLoginPolicy(autoCreateUser, defaultAuthorities, identityType));
        }
        return Map.copyOf(policies);
    }

    /**
     * Coerce a free-form {@code properties.<key>} entry into a
     * {@code List<String>}. Only three shapes are accepted:
     * <ul>
     *   <li>Java array (from programmatic overrides)</li>
     *   <li>{@link List}</li>
     *   <li>{@link Map} &mdash; Spring Boot's relaxed binder materialises
     *       YAML sequences under a {@code Map<String, Object>} bag as
     *       {@link java.util.LinkedHashMap} keyed by {@code "0"},
     *       {@code "1"}, &hellip; so the values collection is the
     *       intended list.</li>
     * </ul>
     * Any other shape is rejected with {@link IllegalArgumentException}
     * so misconfiguration surfaces at startup.
     */
    private static List<String> asStringList(Map<String, Object> properties, String key) {
        if (properties == null) {
            return List.of();
        }
        Object value = properties.get(key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof Object[] arr) {
            return java.util.Arrays.stream(arr).map(String::valueOf).toList();
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().map(String::valueOf).toList();
        }
        throw new IllegalArgumentException("Property '" + key + "' under an " +
                "euler.security.web.login-methods.<name>.properties entry must be " +
                "declared as a YAML list (bound as List, Map, or array); got " +
                value.getClass().getName() + " with value '" + value + "'.");
    }
}
