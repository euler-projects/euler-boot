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

import org.eulerframework.security.provisioning.JitProvisioningPolicy;
import org.eulerframework.security.provisioning.JitProvisioningPolicyResolver;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JitProvisioningPropertiesMapperTests {

    @Test
    public void undeclaredIdentityTypeIsProvisionedWithDefaults() {
        JitProvisioningPolicyResolver resolver = JitProvisioningPropertiesMapper.asResolver(Map.of());

        JitProvisioningPolicy policy = resolver.resolve("phone");

        assertTrue(policy.isEnabled());
        assertEquals(List.of("user"), policy.getDefaultAuthorities());
    }

    @Test
    public void nullDeclarationsAreTreatedAsEmpty() {
        assertTrue(JitProvisioningPropertiesMapper.asResolver(null).resolve("phone").isEnabled());
    }

    @Test
    public void declaredIdentityTypeOverridesAuthorities() {
        EulerBootSecurityProperties.IdentityType corpAccount = identityType(it ->
                it.getJitProvisioning().setDefaultAuthorities(new String[]{"user", "staff"}));

        JitProvisioningPolicyResolver resolver =
                JitProvisioningPropertiesMapper.asResolver(Map.of("corp-account", corpAccount));

        assertEquals(List.of("user", "staff"), resolver.resolve("corp-account").getDefaultAuthorities());
        assertEquals(List.of("user"), resolver.resolve("google").getDefaultAuthorities());
    }

    @Test
    public void explicitlyDisabledIdentityTypeRejectsProvisioning() {
        EulerBootSecurityProperties.IdentityType device = identityType(it ->
                it.getJitProvisioning().setEnabled(false));

        JitProvisioningPolicyResolver resolver =
                JitProvisioningPropertiesMapper.asResolver(Map.of("device", device));

        assertFalse(resolver.resolve("device").isEnabled());
        assertTrue(resolver.resolve("phone").isEnabled());
    }

    @Test
    public void declarationsOfSeveralIdentityTypesAreIndependent() {
        Map<String, EulerBootSecurityProperties.IdentityType> identityTypes = new LinkedHashMap<>();
        identityTypes.put("phone", identityType(it ->
                it.getJitProvisioning().setDefaultAuthorities(new String[]{"user"})));
        identityTypes.put("device", identityType(it ->
                it.getJitProvisioning().setEnabled(false)));

        JitProvisioningPolicyResolver resolver = JitProvisioningPropertiesMapper.asResolver(identityTypes);

        assertTrue(resolver.resolve("phone").isEnabled());
        assertFalse(resolver.resolve("device").isEnabled());
    }

    /**
     * An identity type declared with no settings at all (an empty YAML
     * node) still resolves to the defaults.
     */
    @Test
    public void emptyIdentityTypeDeclarationUsesDefaults() {
        JitProvisioningPolicyResolver resolver = JitProvisioningPropertiesMapper.asResolver(
                Map.of("phone", new EulerBootSecurityProperties.IdentityType()));

        assertTrue(resolver.resolve("phone").isEnabled());
        assertEquals(List.of("user"), resolver.resolve("phone").getDefaultAuthorities());
    }

    private static EulerBootSecurityProperties.IdentityType identityType(
            Consumer<EulerBootSecurityProperties.IdentityType> customizer) {
        EulerBootSecurityProperties.IdentityType identityType =
                new EulerBootSecurityProperties.IdentityType();
        customizer.accept(identityType);
        return identityType;
    }
}
