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
package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * Matches when the {@code euler.security.oauth2.authorizationserver.jwk.keys}
 * map has at least one entry. The standard {@code @ConditionalOnProperty}
 * annotation cannot test "map non-emptiness", so this {@link Condition}
 * inspects the bound configuration tree directly.
 */
class OnJwkKeysConfiguredCondition implements Condition {

    private static final ConfigurationPropertyName KEYS =
            ConfigurationPropertyName.of("euler.security.oauth2.authorizationserver.jwk.keys");

    private static final Bindable<Map<String, Object>> KEYS_BINDABLE =
            Bindable.mapOf(String.class, Object.class);

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Binder binder = Binder.get(context.getEnvironment());
        return binder.bind(KEYS, KEYS_BINDABLE)
                .map(map -> !map.isEmpty())
                .orElse(false);
    }
}
