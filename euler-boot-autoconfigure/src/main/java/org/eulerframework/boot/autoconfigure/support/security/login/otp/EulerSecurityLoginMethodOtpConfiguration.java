/*
 * Copyright 2013-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.autoconfigure.support.security.login.otp;

import org.eulerframework.boot.autoconfigure.support.security.EulerSecurityAuthenticationOtpProperties;
import org.eulerframework.boot.autoconfigure.support.security.login.BaseLoginMethodConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.login.LoginMethodConfiguration;
import org.eulerframework.boot.autoconfigure.support.security.servlet.EulerSecurityWebEndpointProperties;
import org.eulerframework.security.web.login.OtpLoginMethodHandler;
import org.eulerframework.security.web.login.RegisteredLoginMethod;
import org.eulerframework.security.web.login.RegisteredOtpLoginMethod;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({EulerSecurityLoginMethodOtpProperties.class})
public class EulerSecurityLoginMethodOtpConfiguration implements LoginMethodConfiguration {
    private final EulerSecurityLoginMethodOtpProperties properties;

    public EulerSecurityLoginMethodOtpConfiguration(EulerSecurityLoginMethodOtpProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(OtpLoginMethodHandler.class)
    public OtpLoginMethodHandler otpLoginMethodHandler(
            EulerSecurityWebEndpointProperties endpointProperties,
            EulerSecurityAuthenticationOtpProperties otpProperties) {
        return new OtpLoginMethodHandler(
                endpointProperties.getUser().getLoginPage(),
                endpointProperties.getLoginMethods().getDispatch().getMethodParameter(),
                otpProperties.getLoginEndpointUri());
    }

    @Override
    public List<RegisteredLoginMethod> provideRegisteredLoginMethods() {
        List<RegisteredLoginMethod> registered = new ArrayList<>();
        this.properties.getOtp().forEach((id, method) -> {
            if (method == null) {
                return;
            }
            registered.add(new RegisteredOtpLoginMethod(id, name(id, method),
                    method.getIdentityType(), method.isPrimary(), method.getChannel()));
        });
        return registered;
    }

    private String name(String id, BaseLoginMethodConfiguration method) {
        return StringUtils.hasText(method.getMethodName()) ? method.getMethodName() : id;
    }
}
