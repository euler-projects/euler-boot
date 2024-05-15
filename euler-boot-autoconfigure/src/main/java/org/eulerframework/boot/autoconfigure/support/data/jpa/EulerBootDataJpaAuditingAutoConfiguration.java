package org.eulerframework.boot.autoconfigure.support.data.jpa;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import org.eulerframework.security.core.context.UserContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.config.AuditingBeanDefinitionRegistrarSupport;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AutoConfiguration
@ConditionalOnBean(AuditingEntityListener.class)
public class EulerBootDataJpaAuditingAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBean(UserContext.class)
    static class AuditorAwareConfiguration {
        @Component
        static class EulerAuditorAware implements AuditorAware<String> {
            @Resource
            private UserContext userContext;

            @Override
            @Nonnull
            public Optional<String> getCurrentAuditor() {
                return Optional.ofNullable(this.userContext.getUsername());
            }
        }
    }
}
