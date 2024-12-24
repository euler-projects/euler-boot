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
package org.eulerframework.boot.autoconfigure.support.data.jpa;

import jakarta.annotation.Nonnull;
import org.eulerframework.security.core.context.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnBean(AuditingEntityListener.class)
public class EulerBootDataJpaAuditingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(UserContext.class)
    @ConditionalOnClass(AuditorAware.class)
    static class AuditorAwareConfiguration {
        @Component
        static class EulerAuditorAware implements AuditorAware<String> {

            private UserContext userContext;

            @Override
            @Nonnull
            public Optional<String> getCurrentAuditor() {
                return Optional.ofNullable(this.userContext.getUserId());
            }

            @Autowired
            public void setUserContext(UserContext userContext) {
                this.userContext = userContext;
            }
        }
    }
}
