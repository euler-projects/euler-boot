package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.eulerframework.boot.autoconfigure.support.security.oauth2.resource.EulerBootResourceServerAutoConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerBootSecurityWebAutoConfiguration;
import org.eulerframework.security.core.DelegatingPrincipalUserContext;
import org.eulerframework.security.core.UserContext;
import org.eulerframework.security.core.UserDetailsPrincipalUserContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.server.servlet.OAuth2AuthorizationServerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.context.OAuth2AuthenticatedPrincipalUserContext;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.EulerAuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

import static org.springframework.security.config.Customizer.withDefaults;

@AutoConfiguration(before = {
        EulerBootResourceServerAutoConfiguration.class,
        EulerBootSecurityWebAutoConfiguration.class,
        OAuth2AuthorizationServerAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@ConditionalOnClass(EulerAuthorizationServerConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(EulerBootAuthorizationServerProperties.class)
public class EulerBootAuthorizationServerAutoConfiguration {

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http, AuthenticationConfiguration authenticationConfiguration) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        // enable resource owner password credentials grant
        EulerAuthorizationServerConfiguration.configPasswordAuthentication(http, authenticationConfiguration);
        // return original user principal if client support
        // EulerAuthorizationServerConfiguration.configPrincipalSupportTokenIntrospectionAuthentication(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(withDefaults());
        http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
        http.exceptionHandling((exceptions) -> exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/login"), createRequestMatcher()));
        return http.build();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.security.oauth2.authorizationserver", name = "token-store-type", havingValue = "jdbc")
    static class JdbcAuthorizationServerConfiguration {
        @Bean
        @ConditionalOnMissingBean(OAuth2AuthorizationService.class)
        public OAuth2AuthorizationService jdbcOAuth2AuthorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
            return new EulerJdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
        }

        @Bean
        @ConditionalOnMissingBean(OAuth2AuthorizationConsentService.class)
        public OAuth2AuthorizationConsentService jdbcOAuth2AuthorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClientRepository) {
            return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
        }
    }

    @Bean
    @ConditionalOnMissingBean(UserContext.class)
    public UserContext userContext() {
        OAuth2AuthenticatedPrincipalUserContext oauth2AuthenticatedPrincipalUserContext = new OAuth2AuthenticatedPrincipalUserContext();
        UserDetailsPrincipalUserContext userDetailsPrincipalUserContext = new UserDetailsPrincipalUserContext();
        return new DelegatingPrincipalUserContext(oauth2AuthenticatedPrincipalUserContext, userDetailsPrincipalUserContext);
    }

    private static RequestMatcher createRequestMatcher() {
        MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return requestMatcher;
    }
}
