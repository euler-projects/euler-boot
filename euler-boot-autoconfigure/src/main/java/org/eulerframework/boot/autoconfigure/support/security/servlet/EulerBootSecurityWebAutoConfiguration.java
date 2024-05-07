package org.eulerframework.boot.autoconfigure.support.security.servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
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
public class EulerBootSecurityWebAutoConfiguration {
}
