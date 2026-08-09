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
import org.eulerframework.security.core.EulerUserService;
import org.eulerframework.security.core.identity.UserIdentityService;
import org.eulerframework.security.oauth2.client.authentication.OAuth2LoginPrincipalPromotingSuccessHandler;
import org.eulerframework.security.oauth2.client.web.OAuth2LoginMethodHandler;
import org.eulerframework.security.provisioning.JitProvisioningPolicyResolver;
import org.eulerframework.security.web.login.LoginMethodHandler;
import org.eulerframework.security.web.login.RegisteredLoginMethod;
import org.eulerframework.security.web.login.RegisteredOAuth2LoginMethod;
import org.eulerframework.security.web.login.RegisteredLoginMethodRepository;
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
import java.util.Map;

/**
 * Autoconfiguration for the OAuth2 client-side bean backing the
 * {@code euler.security.login-method.oauth2.<method-id>} declarations:
 * {@link OAuth2LoginPrincipalPromotingSuccessHandler}, the success
 * handler that promotes the freshly authenticated OIDC principal into a
 * local {@code EulerUserDetails}, driven by the identity types those
 * same declarations establish.
 *
 * <p>Ordered after Spring Boot's own {@code OAuth2ClientAutoConfiguration}
 * so that {@link ClientRegistrationRepository} (if any) is already in
 * the context by the time the {@link ConditionalOnBean} check is
 * evaluated.
 *
 * <p>The {@code oauth2} login method type itself &mdash; its
 * {@link LoginMethodHandler}, its settings class and the registrations
 * it contributes &mdash; is configured by
 * {@code EulerSecurityLoginMethodOAuth2Configuration} alongside the
 * other types. This class is confined to what only makes sense once an
 * OAuth2 sign-in has actually succeeded.
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

    /**
     * Success handler that promotes the federated principal to a local
     * user, using the {@code registrationId -> identityType} mapping
     * assembled from the
     * {@code euler.security.login-method.oauth2.*} entries and the
     * identity-type keyed
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
            RegisteredLoginMethodRepository registeredLoginMethodRepository,
            JitProvisioningPolicyResolver jitProvisioningPolicyResolver) {
        OAuth2LoginPrincipalPromotingSuccessHandler handler =
                new OAuth2LoginPrincipalPromotingSuccessHandler(userService, userIdentityService);
        handler.setIdentityTypesByRegistrationId(buildIdentityTypes(registeredLoginMethodRepository.findAll()));
        handler.setJitProvisioningPolicyResolver(jitProvisioningPolicyResolver);
        return handler;
    }

    /**
     * Translates every registered {@code oauth2} login method into a
     * {@code registrationId -> identityType} mapping, resolving the
     * registration id exactly as the handler serving those methods does.
     * <p>identity-type is mandatory for oauth2 entries (fail fast).
     */
    private static Map<String, String> buildIdentityTypes(
            Collection<RegisteredLoginMethod> loginMethods) {
        if (loginMethods == null || loginMethods.isEmpty()) {
            return Map.of();
        }
        Map<String, String> identityTypes = new LinkedHashMap<>();
        for (RegisteredLoginMethod method : loginMethods) {
            if (!(method instanceof RegisteredOAuth2LoginMethod oauth2Method)) {
                continue;
            }
            String identityType = oauth2Method.getIdentityType();
            if (identityType == null || identityType.isEmpty()) {
                throw new IllegalStateException("Login method '" + oauth2Method.getId()
                        + "' (type=oauth2) requires identity-type but none is declared.");
            }
            String provider = OAuth2LoginMethodHandler.resolveProvider(oauth2Method);
            identityTypes.put(OAuth2LoginMethodHandler.resolveRegistrationId(oauth2Method, provider),
                    identityType);
        }
        return Map.copyOf(identityTypes);
    }
}
