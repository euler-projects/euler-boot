package org.eulerframework.boot.autoconfigure.support.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.Arrays;

@AutoConfiguration
@ConditionalOnClass(WebSecurity.class)
@EnableConfigurationProperties({
        EulerBootSecurityProperties.class
})
public class EulerBootSecurityAutoConfiguration {

    @ConditionalOnClass(WebSecurity.class)
    @Order(SecurityProperties.IGNORED_ORDER)
    //@Configuration
    public static class IgnoredPathsWebSecurityConfigurer
            implements WebSecurityConfigurer<WebSecurity> {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        @Autowired
        private EulerBootSecurityProperties eulerBootSecurityProperties;

        @Override
        public void init(WebSecurity builder) {
            String[] ignoredPatterns = this.eulerBootSecurityProperties.getIgnoredPatterns();
            if(ignoredPatterns != null && ignoredPatterns.length > 0) {
                if(this.logger.isInfoEnabled()) {
                    this.logger.info("config ignored patterns: {}", Arrays.toString(ignoredPatterns));
                }

                builder.ignoring().requestMatchers(
                        Arrays.stream(ignoredPatterns)
                                .map(AntPathRequestMatcher::new)
                                .toArray(AntPathRequestMatcher[]::new));
            }
        }

        @Override
        public void configure(WebSecurity builder) {
        }

    }
}
