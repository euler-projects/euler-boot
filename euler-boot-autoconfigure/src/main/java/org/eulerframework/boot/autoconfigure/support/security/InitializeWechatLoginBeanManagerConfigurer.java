package org.eulerframework.boot.autoconfigure.support.security;

import org.eulerframework.boot.autoconfigure.support.security.oauth2.resource.EulerBootResourceServerProperties;
import org.eulerframework.boot.autoconfigure.support.security.oauth2.server.EulerBootAuthorizationServerProperties;
import org.eulerframework.security.authentication.WechatLoginCodeAuthenticationProvider;
import org.eulerframework.security.core.userdetails.EulerWechatUserDetailsService;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;

@Order(InitializeWechatLoginBeanManagerConfigurer.DEFAULT_ORDER)
public class InitializeWechatLoginBeanManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
    static final int DEFAULT_ORDER = Ordered.LOWEST_PRECEDENCE - 4000;

    private final ApplicationContext context;

    InitializeWechatLoginBeanManagerConfigurer(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void init(AuthenticationManagerBuilder auth) throws Exception {
        auth.apply(new InitializeWechatLoginManagerConfigurer());
    }

    class InitializeWechatLoginManagerConfigurer extends GlobalAuthenticationConfigurerAdapter {
        @Override
        public void configure(AuthenticationManagerBuilder auth) throws Exception {
            String[] beanNames = InitializeWechatLoginBeanManagerConfigurer.this.context
                    .getBeanNamesForType(EulerWechatUserDetailsService.class);
            if (beanNames.length == 0) {
                return;
            }

            EulerBootAuthorizationServerProperties properties = InitializeWechatLoginBeanManagerConfigurer.this.context
                    .getBean(EulerBootAuthorizationServerProperties.class);

            if(!properties.getWechatLogin().isEnabled()) {
                return;
            }

            EulerWechatUserDetailsService wechatUserDetailsService = InitializeWechatLoginBeanManagerConfigurer.this.context
                    .getBean(beanNames[0], EulerWechatUserDetailsService.class);
            WechatLoginCodeAuthenticationProvider provider = new WechatLoginCodeAuthenticationProvider();
            provider.setWechatUserDetailsService(wechatUserDetailsService);
            provider.setAutoCreateUserIfNotExists(properties.getWechatLogin().isAutoCreateUserIfNotExists());
            auth.authenticationProvider(provider);
        }
    }
}
