/*
 * Copyright 2013-2024 the original author or authors.
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
package org.eulerframework.boot.autoconfigure.support.web.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebI18nProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebSiteProperties;
import org.eulerframework.common.util.json.JacksonUtils;
import org.eulerframework.web.core.base.response.ErrorResponse;
import org.eulerframework.web.core.i18n.ClassPathReloadableResourceBundleMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;
import java.time.Duration;
import java.util.Date;

@AutoConfiguration(
        before = {
                MessageSourceAutoConfiguration.class
        }
)
@EnableConfigurationProperties({
        EulerCacheProperties.class,
        EulerWebSiteProperties.class,
        EulerWebI18nProperties.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EulerWebSupportAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(EulerWebSupportAutoConfiguration.class);

    @Bean
    public ObjectMapper objectMapper() {
        this.logger.debug("Create ObjectMapper use JacksonUtils");
        return JacksonUtils.getDefaultObjectMapper();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.messages")
    public MessageSourceProperties messageSourceProperties() {
        return new MessageSourceProperties();
    }

    @Bean
    @ConditionalOnClass(ClassPathReloadableResourceBundleMessageSource.class)
    public MessageSource messageSource(MessageSourceProperties properties) {
        ClassPathReloadableResourceBundleMessageSource messageSource = new ClassPathReloadableResourceBundleMessageSource();
        if (StringUtils.hasText(properties.getBasename())) {
            messageSource.setBasenames(StringUtils
                    .commaDelimitedListToStringArray(StringUtils.trimAllWhitespace(properties.getBasename())));
        }
        if (properties.getEncoding() != null) {
            messageSource.setDefaultEncoding(properties.getEncoding().name());
        }
        messageSource.setFallbackToSystemLocale(properties.isFallbackToSystemLocale());
        Duration cacheDuration = properties.getCacheDuration();
        if (cacheDuration != null) {
            messageSource.setCacheMillis(cacheDuration.toMillis());
        }
        messageSource.setAlwaysUseMessageFormat(properties.isAlwaysUseMessageFormat());
        messageSource.setUseCodeAsDefaultMessage(properties.isUseCodeAsDefaultMessage());
        return messageSource;
    }

    @Bean
    public EulerBootPropertySource eulerBootPropertySource(
            ConfigurableEnvironment configurableEnvironment,
            MultipartProperties multipartProperties,
            EulerApplicationProperties eulerApplicationProperties,
            EulerWebSiteProperties eulerWebSiteProperties,
            EulerWebI18nProperties eulerWebI18nProperties,
            EulerCacheProperties eulerCacheProperties) {
        return new EulerBootPropertySource(configurableEnvironment, multipartProperties, eulerApplicationProperties, eulerWebSiteProperties, eulerWebI18nProperties, eulerCacheProperties);
    }

    @ControllerAdvice
    public static class GlobalControllerAdvice {

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

    @RestControllerAdvice
    public static class GlobalRestControllerAdvice {
        private final Logger logger = LoggerFactory.getLogger(GlobalRestControllerAdvice.class);

        /**
         * 用于在程序发生{@link Exception}异常时统一返回错误信息
         *
         * @return 包含错误信息的Ajax响应体
         */
        @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
        @ExceptionHandler(Exception.class)
        public Object exception(Exception e) {
            this.logger.error(e.getMessage(), e);
            if (e instanceof org.springframework.web.ErrorResponse) {
                return e;
            }
            return new ErrorResponse();
        }
    }
}
