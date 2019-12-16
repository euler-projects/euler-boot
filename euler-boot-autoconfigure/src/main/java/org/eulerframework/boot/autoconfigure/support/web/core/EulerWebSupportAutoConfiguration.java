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
package org.eulerframework.boot.autoconfigure.support.web.core;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebProperties;
import org.eulerframework.web.core.i18n.ClassPathReloadableResourceBundleMessageSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Configuration
@EnableConfigurationProperties({
        EulerCacheProperties.class,
        EulerWebProperties.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EulerWebSupportAutoConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Bean
    @ConditionalOnClass(ClassPathReloadableResourceBundleMessageSource.class)
    public MessageSource messageSource(EulerWebProperties eulerWebProperties) {
        ClassPathReloadableResourceBundleMessageSource messageSource = new ClassPathReloadableResourceBundleMessageSource();
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setBasename(eulerWebProperties.getI18n().getResourcePath());
        return messageSource;
    }

    @Bean
    public EulerBootPropertySource springBootPropertySource(
            ConfigurableEnvironment configurableEnvironment,
            EulerApplicationProperties eulerApplicationProperties,
            EulerWebProperties eulerWebProperties,
            EulerCacheProperties eulerCacheProperties) {
        return new EulerBootPropertySource(configurableEnvironment, eulerApplicationProperties, eulerWebProperties, eulerCacheProperties);
    }

    @ControllerAdvice
    public static class GlobalParameterBinder {
        /**
         * 尝试以时间戳的方式格式化时间,如果失败则传递原始字符串
         *
         * @param binder
         */
        @InitBinder
        public void initBinder(WebDataBinder binder) {
            binder.registerCustomEditor(Date.class, new PropertyEditorSupport() {
                @Override
                public void setAsText(String value) {
                    if (StringUtils.hasText(value)) {
                        try {
                            long timestamp = Long.parseLong(value);
                            setValue(new Date(timestamp));
                        } catch (NumberFormatException e) {
                            setValue(value);
                        }
                    } else {
                        setValue(value);
                    }
                }
            });
        }
    }
}
