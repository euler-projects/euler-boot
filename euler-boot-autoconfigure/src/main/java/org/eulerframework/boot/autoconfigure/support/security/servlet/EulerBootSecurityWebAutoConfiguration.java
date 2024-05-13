package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.eulerframework.security.core.userdetails.EulerUserDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = {
        SecurityAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@EnableConfigurationProperties(EulerBootSecurityWebProperties.class)
@ConditionalOnClass(EulerUserDetails.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        EulerBootWebSecurityConfiguration.class,
        EulerUserDetailsServiceConfiguration.class
})
public class EulerBootSecurityWebAutoConfiguration {
}
