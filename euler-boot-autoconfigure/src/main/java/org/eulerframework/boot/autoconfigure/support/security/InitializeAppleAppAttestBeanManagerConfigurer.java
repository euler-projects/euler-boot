package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.EulerBootAuthorizationServerProperties;
import org.eulerframework.security.authentication.apple.AppleAppAttestAssertionAuthenticationProvider;
import org.eulerframework.security.authentication.apple.AppleAppAttestAttestationAuthenticationProvider;
import org.eulerframework.security.authentication.apple.AppleAppAttestKeyCredentialService;
import org.eulerframework.security.authentication.apple.AppleAppAttestValidationService;
import org.eulerframework.security.authentication.apple.AppleAppRepository;
import org.eulerframework.security.authentication.apple.DefaultAppleAppAttestValidationService;
import org.eulerframework.security.authentication.apple.InMemoryAppleAppRepository;
import org.eulerframework.security.authentication.apple.RegisteredAppleApp;
import org.eulerframework.security.core.userdetails.EulerAppleAppAttestUserDetailsService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Order(InitializeAppleAppAttestBeanManagerConfigurer.DEFAULT_ORDER)
public class InitializeAppleAppAttestBeanManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
    static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 4000;

    private final ApplicationContext context;

    InitializeAppleAppAttestBeanManagerConfigurer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(AuthenticationManagerBuilder auth) {
        auth.apply(new InitializeAppleAppAttestManagerConfigurer());
    }

    class InitializeAppleAppAttestManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void configure(AuthenticationManagerBuilder auth) {
            String[] userDetailsServiceBeanNames = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBeanNamesForType(EulerAppleAppAttestUserDetailsService.class);
            if (userDetailsServiceBeanNames.length == 0) {
                return;
            }

            EulerBootAuthorizationServerProperties properties = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBean(EulerBootAuthorizationServerProperties.class);

            if (!properties.getAppleAppAttest().isEnabled()) {
                return;
            }

            EulerAppleAppAttestUserDetailsService userDetailsService = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBean(userDetailsServiceBeanNames[0], EulerAppleAppAttestUserDetailsService.class);

            AppleAppAttestKeyCredentialService keyCredentialService = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBean(AppleAppAttestKeyCredentialService.class);

            AppleAppAttestValidationService validationService = getOrCreateValidationService(properties, keyCredentialService);

            // Register attestation (initial device registration) provider
            AppleAppAttestAttestationAuthenticationProvider attestationProvider = new AppleAppAttestAttestationAuthenticationProvider(
                    validationService, userDetailsService, keyCredentialService);
            attestationProvider.setAutoCreateUserIfNotExists(properties.getAppleAppAttest().isAutoCreateUserIfNotExists());
            auth.authenticationProvider(attestationProvider);

            // Register assertion (device re-authentication) provider
            AppleAppAttestAssertionAuthenticationProvider assertionProvider = new AppleAppAttestAssertionAuthenticationProvider(
                    validationService, userDetailsService);
            auth.authenticationProvider(assertionProvider);
        }

        private AppleAppAttestValidationService getOrCreateValidationService(
                EulerBootAuthorizationServerProperties properties,
                AppleAppAttestKeyCredentialService keyCredentialService) {
            String[] validationServiceBeanNames = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBeanNamesForType(AppleAppAttestValidationService.class);
            if (validationServiceBeanNames.length > 0) {
                return InitializeAppleAppAttestBeanManagerConfigurer.this.context
                        .getBean(validationServiceBeanNames[0], AppleAppAttestValidationService.class);
            }

            EulerBootAuthorizationServerProperties.AppleAppAttest appleAppAttestProperties = properties.getAppleAppAttest();
            AppleAppRepository appRepository = getOrCreateAppleAppRepository(appleAppAttestProperties);
            DefaultAppleAppAttestValidationService validationService = new DefaultAppleAppAttestValidationService(
                    appRepository, keyCredentialService);
            validationService.setAllowDevelopmentEnvironment(appleAppAttestProperties.isAllowDevelopmentEnvironment());
            return validationService;
        }

        private AppleAppRepository getOrCreateAppleAppRepository(
                EulerBootAuthorizationServerProperties.AppleAppAttest appleAppAttestProperties) {
            String[] appRepositoryBeanNames = InitializeAppleAppAttestBeanManagerConfigurer.this.context
                    .getBeanNamesForType(AppleAppRepository.class);
            if (appRepositoryBeanNames.length > 0) {
                return InitializeAppleAppAttestBeanManagerConfigurer.this.context
                        .getBean(appRepositoryBeanNames[0], AppleAppRepository.class);
            }

            Map<String, EulerBootAuthorizationServerProperties.AppleAppAttest.AppRegistration> appMap =
                    appleAppAttestProperties.getApps();
            List<RegisteredAppleApp> registeredApps = new ArrayList<>();
            appMap.forEach((name, appProps) ->
                    registeredApps.add(new RegisteredAppleApp(appProps.getTeamId(), appProps.getBundleId())));
            return new InMemoryAppleAppRepository(registeredApps);
        }
    }
}
