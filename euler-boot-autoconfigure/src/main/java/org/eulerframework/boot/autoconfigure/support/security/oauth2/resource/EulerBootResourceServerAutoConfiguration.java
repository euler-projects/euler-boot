package org.eulerframework.boot.autoconfigure.support.security.oauth2.resource;

import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

@AutoConfiguration(
        before = {
                EulerBootSecurityWebAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        })
@EnableConfigurationProperties(EulerBootResourceServerProperties.class)
@ConditionalOnClass(BearerTokenAuthenticationToken.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        EulerBootResourceServerConfiguration.LocalAuthorizationServerResourceServerConfiguration.class,
        EulerBootResourceServerConfiguration.OpaqueTokenResourceServerConfiguration.class,
        EulerBootResourceServerConfiguration.JwkSetUriResourceServerConfiguration.class,
        EulerBootResourceServerConfiguration.KeyValueJwtResourceServerConfiguration.class
})
public class EulerBootResourceServerAutoConfiguration {

}
