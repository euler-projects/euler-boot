package org.eulerframework.boot.autoconfigure.support.security.oauth.server;

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityProperties;
import org.eulerframework.security.core.EulerUserService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.EulerAuthorizationServerConfig;

@AutoConfiguration
@ConditionalOnBean(EulerUserService.class)
@EnableConfigurationProperties({
        EulerBootSecurityProperties.class
})
@Import(EulerAuthorizationServerConfig.class)
public class EulerBootAuthorizationServerAutoConfiguration {

}
