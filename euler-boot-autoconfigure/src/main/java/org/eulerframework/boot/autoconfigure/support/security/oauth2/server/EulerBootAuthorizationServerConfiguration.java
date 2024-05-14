package org.eulerframework.boot.autoconfigure.support.security.oauth2.server;

import org.eulerframework.boot.autoconfigure.support.security.SecurityFilterChainBeanNames;
import org.eulerframework.security.core.context.DelegatingUserContext;
import org.eulerframework.security.core.context.UserContext;
import org.eulerframework.security.web.context.UsernamePasswordAuthenticationUserContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.eulerframework.security.oauth2.resource.context.BearerTokenAuthenticationUserContext;
import org.springframework.security.oauth2.server.authorization.EulerJdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
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

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EulerAuthorizationServerConfiguration.class)
public class EulerBootAuthorizationServerConfiguration {

    @Bean(SecurityFilterChainBeanNames.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN)
    @ConditionalOnMissingBean(name = SecurityFilterChainBeanNames.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN)
    @Order(Ordered.HIGHEST_PRECEDENCE)
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
        BearerTokenAuthenticationUserContext oauth2AuthenticatedPrincipalUserContext = new BearerTokenAuthenticationUserContext();
        UsernamePasswordAuthenticationUserContext usernamePasswordAuthenticationUserContext = new UsernamePasswordAuthenticationUserContext();
        return new DelegatingUserContext(oauth2AuthenticatedPrincipalUserContext, usernamePasswordAuthenticationUserContext);
    }

    private static RequestMatcher createRequestMatcher() {
        MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return requestMatcher;
    }
}
