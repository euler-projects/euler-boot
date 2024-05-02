/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.autoconfigure.support.web.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Configuration;

@Configuration
//@ConditionalOnClass(WebSecurityConfigurer.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class EulerBootWebSecurityConfiguration {

//    @Configuration
//    @Order(SecurityProperties.BASIC_AUTH_ORDER)
//    static class EulerBootWebSecurityConfigurerAdapter implements WebSecurityConfigurer {

//        @Autowired
//        private UserDetailsService userDetailsService;
//        @Autowired
//        private PasswordEncoder passwordEncoder;
//        @Autowired(required = false)
//        private List<OrderedAuthenticationProvider> additionalAuthenticationProviders;
//
//
//        @Autowired
//        private SessionRegistry sessionRegistry;
//        @Autowired
//        private AuthenticationEntryPoint authenticationEntryPoint;
//        @Autowired
//        private AntPathRequestMatcher requiresAuthenticationRequestMatcher;
//        @Autowired
//        private AuthenticationSuccessHandler authenticationSuccessHandler;
//        @Autowired
//        private AuthenticationFailureHandler authenticationFailureHandler;
//
//        @Override
//        public void init(SecurityBuilder builder) throws Exception {
//
//        }
//
//        @Override
//        public void configure(SecurityBuilder builder) throws Exception {
//
//        }
//
//        public interface OrderedAuthenticationProvider extends AuthenticationProvider {
//            int order();
//        }

//        @Override
//        protected void configure(AuthenticationManagerBuilder auth) {
//            DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
//            daoAuthenticationProvider.setUserDetailsService(userDetailsService);
//            daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
//            auth.authenticationProvider(daoAuthenticationProvider);
//
//            Optional.ofNullable(this.additionalAuthenticationProviders).orElse(Collections.emptyList())
//                    .stream()
//                    .sorted(Comparator.comparingInt(OrderedAuthenticationProvider::order))
//                    .forEach(auth::authenticationProvider);
//        }
//
//        @Override
//        protected void configure(HttpSecurity http) throws Exception {
//            http.authorizeRequests()
//                    .antMatchers("/demo/**").authenticated()
//                    .and()
//                    .exceptionHandling()
//                    .defaultAuthenticationEntryPointFor(this.authenticationEntryPoint, new AntPathRequestMatcher("/**", null))
//                    .and()
//                    .addFilterAt(this.formLoginFilter(), UsernamePasswordAuthenticationFilter.class)
//                    .logout()
//                    .logoutUrl("/signout")
//                    .logoutSuccessUrl("/")
//                    .invalidateHttpSession(true)
//                    .deleteCookies("JSESSIONID")
//                    .and()
//                    .csrf().disable()
//                    .headers()
//                    .cacheControl().disable()
//                    .contentTypeOptions()
//                    .and()
//                    .httpStrictTransportSecurity()
//                    .and()
//                    .frameOptions().sameOrigin()
//                    .xssProtection()
//                    .and()
//                    .and()
//                    .sessionManagement()
//                    .invalidSessionUrl("/")
//                    .sessionFixation().migrateSession()
//                    .maximumSessions(20)
//                    //.expiredSessionStrategy()
//                    .sessionRegistry(this.sessionRegistry);
//        }
//
//        @Bean(name = "authenticationManager")
//        public AuthenticationManager authenticationManagerBean() throws Exception {
//            return super.authenticationManagerBean();
//        }
//
//        @Bean(name = "formLoginFilter")
//        public UsernamePasswordAuthenticationFilter formLoginFilter() throws Exception {
//            CaptchaUsernamePasswordAuthenticationFilter captchaUsernamePasswordAuthenticationFilter = new CaptchaUsernamePasswordAuthenticationFilter();
//            captchaUsernamePasswordAuthenticationFilter.setEnableCaptcha(SecurityConfig.isSignUpEnableCaptcha());
//            captchaUsernamePasswordAuthenticationFilter.setAuthenticationManager(this.authenticationManager());
//            captchaUsernamePasswordAuthenticationFilter.setRequiresAuthenticationRequestMatcher(this.requiresAuthenticationRequestMatcher);
//            captchaUsernamePasswordAuthenticationFilter.setAuthenticationSuccessHandler(this.authenticationSuccessHandler);
//            captchaUsernamePasswordAuthenticationFilter.setAuthenticationFailureHandler(this.authenticationFailureHandler);
//            return captchaUsernamePasswordAuthenticationFilter;
//        }
//    }

//    @Bean
//    @ConditionalOnMissingBean(AccessDeniedHandler.class)
//    public AccessDeniedHandler accessDeniedHandler() {
//        return new EulerAccessDeniedHandler();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(SessionRegistry.class)
//    public SessionRegistry sessionRegistry() {
//        return new SessionRegistryImpl();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
//    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
//        return new EulerLoginUrlAuthenticationEntryPoint(
//                SecurityConfig.getLoginPage(),
//                objectMapper
//        );
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(RequestMatcher.class)
//    public AntPathRequestMatcher requiresAuthenticationRequestMatcher() {
//        return new AntPathRequestMatcher(
//                SecurityConfig.getLoginProcessingUrl(),
//                "POST"
//        );
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(AuthenticationSuccessHandler.class)
//    public AuthenticationSuccessHandler authenticationSuccessHandler() {
//        SimpleUrlAuthenticationSuccessHandler authenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler();
//        authenticationSuccessHandler.setDefaultTargetUrl(SecurityConfig.getLoginDefaultTargetUrl());
//        authenticationSuccessHandler.setTargetUrlParameter("redirectUrl");
//        return authenticationSuccessHandler;
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(AuthenticationFailureHandler.class)
//    public AuthenticationFailureHandler authenticationFailureHandler() {
//        return new EulerUrlAuthenticationFailureHandler();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(PasswordEncoder.class)
//    public PasswordEncoder passwordEncoder() {
//        String encodingId = "bcrypt";
//        Map<String, PasswordEncoder> encoders = new HashMap<>();
//        encoders.put(encodingId, new BCryptPasswordEncoder());
////        encoders.put("ldap", new org.springframework.security.crypto.password.LdapShaPasswordEncoder());
////        encoders.put("MD4", new org.springframework.security.crypto.password.Md4PasswordEncoder());
////        encoders.put("MD5", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("MD5"));
////        encoders.put("noop", org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance());
////        encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
////        encoders.put("scrypt", new SCryptPasswordEncoder());
////        encoders.put("SHA-1", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-1"));
////        encoders.put("SHA-256", new org.springframework.security.crypto.password.MessageDigestPasswordEncoder("SHA-256"));
////        encoders.put("sha256", new org.springframework.security.crypto.password.StandardPasswordEncoder());
//
//        encoders.put("plain-text", new PlainTextPasswordEncoder());
//
//        return new DelegatingPasswordEncoder(encodingId, encoders);
//    }
//
//    static class PlainTextPasswordEncoder implements PasswordEncoder {
//        @Override
//        public String encode(CharSequence rawPassword) {
//            return rawPassword == null ? null : rawPassword.toString();
//        }
//
//        @Override
//        public boolean matches(CharSequence rawPassword, String encodedPassword) {
//            return rawPassword != null && rawPassword.equals(encodedPassword);
//        }
//    }

}
