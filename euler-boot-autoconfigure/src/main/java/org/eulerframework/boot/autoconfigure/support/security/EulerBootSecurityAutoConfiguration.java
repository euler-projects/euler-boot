package org.eulerframework.boot.autoconfigure.support.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

@AutoConfiguration(before = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@EnableConfigurationProperties({
        EulerBootSecurityProperties.class
})
@ConditionalOnClass(DefaultAuthenticationEventPublisher.class)
public class EulerBootSecurityAutoConfiguration {
}
