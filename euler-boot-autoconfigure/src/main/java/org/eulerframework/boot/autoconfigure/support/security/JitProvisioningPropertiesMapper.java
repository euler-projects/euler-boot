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
package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.security.provisioning.jit.JitProvisioningPolicy;
import org.eulerframework.security.provisioning.jit.JitProvisioningPolicyResolver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps {@link JitProvisioning} settings to the framework-level
 * {@link JitProvisioningPolicy} and
 * {@link JitProvisioningPolicyResolver}.
 */
public final class JitProvisioningPropertiesMapper {

    private JitProvisioningPropertiesMapper() {
    }

    /**
     * Returns the policy declared by the given settings.
     *
     * @param jitProvisioning the declared settings; {@code null} yields
     *                        the disabled policy
     * @throws IllegalArgumentException if provisioning is enabled but no
     *                                  authority is granted
     */
    public static JitProvisioningPolicy asPolicy(JitProvisioning jitProvisioning) {
        if (jitProvisioning == null || !jitProvisioning.isEnabled()) {
            return JitProvisioningPolicy.disabled();
        }
        String[] defaultAuthorities = jitProvisioning.getDefaultAuthorities();
        return JitProvisioningPolicy.enabled(defaultAuthorities == null
                ? List.of() : List.of(defaultAuthorities));
    }

    /**
     * Builds a resolver from the per-identity-type declarations under
     * {@code euler.security.identity-type}. Identity types without a
     * declaration resolve to the defaults of a fresh
     * {@link JitProvisioning} (enabled, authorities {@code [user]}), so
     * provisioning is on unless explicitly disabled.
     *
     * @param identityTypes the declared settings keyed by identity type;
     *                      {@code null} is treated as empty
     */
    public static JitProvisioningPolicyResolver asResolver(
            Map<String, EulerSecurityProperties.IdentityType> identityTypes) {
        JitProvisioningPolicy defaultPolicy = asPolicy(new JitProvisioning());
        Map<String, JitProvisioningPolicy> policies = new LinkedHashMap<>();
        if (identityTypes != null) {
            identityTypes.forEach((identityType, declaration) ->
                    policies.put(identityType, declaration == null
                            ? defaultPolicy : asPolicy(declaration.getJitProvisioning())));
        }
        return identityType -> policies.getOrDefault(identityType, defaultPolicy);
    }
}
