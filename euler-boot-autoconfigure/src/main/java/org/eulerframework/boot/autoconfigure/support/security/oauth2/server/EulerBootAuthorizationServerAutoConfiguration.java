package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.eulerframework.boot.autoconfigure.support.security.oauth2.resource.EulerBootResourceServerAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

@AutoConfiguration(before = {
        EulerBootResourceServerAutoConfiguration.class,
        EulerBootSecurityWebAutoConfiguration.class,
        OAuth2AuthorizationServerAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@EnableConfigurationProperties(EulerBootAuthorizationServerProperties.class)
@ConditionalOnClass(OAuth2Authorization.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(EulerBootAuthorizationServerConfiguration.class)
public class EulerBootAuthorizationServerAutoConfiguration {

}
