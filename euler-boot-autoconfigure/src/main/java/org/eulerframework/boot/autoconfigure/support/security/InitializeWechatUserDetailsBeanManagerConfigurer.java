package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.EulerBootAuthorizationServerProperties;
import org.eulerframework.security.authentication.WechatAuthorizationCodeAuthenticationProvider;
import org.eulerframework.security.core.userdetails.EulerWechatUserDetailsService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

@Order(InitializeWechatUserDetailsBeanManagerConfigurer.DEFAULT_ORDER)
public class InitializeWechatUserDetailsBeanManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
    static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 4000;

    private final ApplicationContext context;

    InitializeWechatUserDetailsBeanManagerConfigurer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(AuthenticationManagerBuilder auth) {
        auth.apply(new InitializeWechatUserDetailsManagerConfigurer());
    }

    class InitializeWechatUserDetailsManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void configure(AuthenticationManagerBuilder auth) {
            String[] beanNames = InitializeWechatUserDetailsBeanManagerConfigurer.this.context
                    .getBeanNamesForType(EulerWechatUserDetailsService.class);
            if (beanNames.length == 0) {
                return;
            }

            EulerBootAuthorizationServerProperties properties = InitializeWechatUserDetailsBeanManagerConfigurer.this.context
                    .getBean(EulerBootAuthorizationServerProperties.class);

            if (!properties.getWechatLogin().isEnabled()) {
                return;
            }

            EulerWechatUserDetailsService wechatUserDetailsService = InitializeWechatUserDetailsBeanManagerConfigurer.this.context
                    .getBean(beanNames[0], EulerWechatUserDetailsService.class);
            WechatAuthorizationCodeAuthenticationProvider provider = new WechatAuthorizationCodeAuthenticationProvider(
                    properties.getWechatLogin().getCode2SessionEndpoint(),
                    properties.getWechatLogin().getAppid(),
                    properties.getWechatLogin().getSecret()
            );
            provider.setWechatUserDetailsService(wechatUserDetailsService);
            provider.setAutoCreateUserIfNotExists(properties.getWechatLogin().isAutoCreateUserIfNotExists());
            auth.authenticationProvider(provider);
        }
    }
}
