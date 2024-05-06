package org.eulerframework.boot.autoconfigure.support.security.oauth.server;

import org.eulerframework.boot.autoconfigure.support.security.EulerBootSecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.EulerAuthorizationServerConfig;

@AutoConfiguration
@EnableConfigurationProperties({
        EulerBootSecurityProperties.class
})
@ConditionalOnClass(EulerAuthorizationServerConfig.class)
@Import(EulerAuthorizationServerConfig.class)
public class EulerBootAuthorizationServerAutoConfiguration {

}
