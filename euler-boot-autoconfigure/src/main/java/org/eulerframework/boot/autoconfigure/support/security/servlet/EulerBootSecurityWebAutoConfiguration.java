package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.core.EulerUserService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@Import({
        EulerBootWebSecurityConfiguration.class
})
@ConditionalOnBean(EulerUserService.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EulerBootSecurityWebAutoConfiguration {
}
