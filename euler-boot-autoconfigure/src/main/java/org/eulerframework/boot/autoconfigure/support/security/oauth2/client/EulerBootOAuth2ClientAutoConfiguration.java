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
import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityProperties;
import org.eulerframework.boot.autoconfigure.support.security.servlet.LoginMethodPropertiesMapper;
import org.eulerframework.common.util.collections.MapUtils;
import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.oauth2.client.authentication.OAuth2LoginPrincipalPromotingSuccessHandler;
import org.eulerframework.security.oauth2.client.web.OAuth2LoginMethodHandler;
import org.eulerframework.security.provisioning.JitProvisioningPolicyResolver;
import org.eulerframework.security.web.endpoint.user.login.RegisteredLoginMethod;
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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Autoconfiguration for OAuth2 client-side beans backing the unified
 * {@code euler.security.login-method.<name>.type: oauth2}
 * declaration:
 *
 * <ul>
 *   <li>{@link OAuth2LoginMethodHandler} &mdash; turns each
 *       {@code type: oauth2} config entry into an available login
 *       method by resolving the referenced
 *       {@code spring.security.oauth2.client.registration.<key>} entry.</li>
 *   <li>{@link OAuth2LoginPrincipalPromotingSuccessHandler} &mdash; the
 *       success handler that promotes the freshly authenticated OIDC
 *       principal into a local {@code EulerUserDetails}, driven by a
 *       per-registration policy assembled from the same
 *       {@code login-method} entries.</li>
 * </ul>
 *
 * <p>Ordered after Spring Boot's own {@code OAuth2ClientAutoConfiguration}
 * so that {@link ClientRegistrationRepository} (if any) is already in
 * the context by the time the {@link ConditionalOnBean} check is
 * evaluated.
 *
 * <p>The generic {@code LoginMethodContributor} dispatcher that iterates
 * {@code login-method} entries and delegates to
 * {@link org.eulerframework.security.web.endpoint.user.login.LoginMethodHandler}s
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
    @ConditionalOnMissingBean(OAuth2LoginMethodHandler.class)
    public OAuth2LoginMethodHandler oauth2LoginMethodHandler(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new OAuth2LoginMethodHandler(clientRegistrationRepository);
    }

    /**
     * Success handler that promotes the federated principal to a local
     * user, using the {@code registrationId -> identityType} mapping
     * assembled from
     * {@code euler.security.login-method.*} entries whose
     * {@code method-type == oauth2} and the identity-type keyed
     * {@link JitProvisioningPolicyResolver}.
     *
     * <p>Registrations declared under
     * {@code spring.security.oauth2.client.registration.*} but not
     * mentioned in {@code login-method} receive no mapping entry, so a
     * stray non-login registration accessed via
     * {@code /oauth2/authorization/...} will only sign in already-known
     * users.
     */
    @Bean
    @ConditionalOnMissingBean(OAuth2LoginPrincipalPromotingSuccessHandler.class)
    public OAuth2LoginPrincipalPromotingSuccessHandler oauth2LoginPrincipalPromotingSuccessHandler(
            EulerUserService userService,
            UserIdentityService userIdentityService,
            EulerBootSecurityProperties securityProperties,
            JitProvisioningPolicyResolver jitProvisioningPolicyResolver) {
        OAuth2LoginPrincipalPromotingSuccessHandler handler =
                new OAuth2LoginPrincipalPromotingSuccessHandler(userService, userIdentityService);
        handler.setIdentityTypesByRegistrationId(buildIdentityTypes(
                new LoginMethodPropertiesMapper(securityProperties).asRegisteredLoginMethods()));
        handler.setJitProvisioningPolicyResolver(jitProvisioningPolicyResolver);
        return handler;
    }

    /**
     * Translates every {@code method-type=oauth2} registered login
     * method into a {@code registrationId -> identityType} mapping.
     * <p>identity-type is mandatory for oauth2 entries (fail fast).
     * Registration ID resolves as: explicit
     * {@code oauth-client-registration-id} ?? provider (provider ??
     * identity-type). No fallback to the login-method key.
     */
    private static Map<String, String> buildIdentityTypes(
            Collection<RegisteredLoginMethod> loginMethods) {
        if (loginMethods == null || loginMethods.isEmpty()) {
            return Map.of();
        }
        Map<String, String> identityTypes = new LinkedHashMap<>();
        for (RegisteredLoginMethod method : loginMethods) {
            if (method == null || !OAuth2LoginMethodHandler.TYPE.equals(method.getType())) {
                continue;
            }
            String identityType = method.getIdentityType();
            if (identityType == null || identityType.isEmpty()) {
                throw new IllegalStateException("Login method '" + method.getId()
                        + "' (type=oauth2) requires identity-type but none is declared.");
            }
            String provider = MapUtils.getString(method.getProperties(),
                    OAuth2LoginMethodHandler.PROP_PROVIDER);
            if (provider == null || provider.isEmpty()) {
                provider = identityType;
            }
            String registrationId = MapUtils.getString(method.getProperties(),
                    OAuth2LoginMethodHandler.PROP_OAUTH_CLIENT_REGISTRATION_ID);
            if (registrationId == null || registrationId.isEmpty()) {
                registrationId = provider;
            }
            identityTypes.put(registrationId, identityType);
        }
        return Map.copyOf(identityTypes);
    }
}
