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
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCopyrightProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebI18nProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebSiteProperties;
import org.eulerframework.common.util.json.JacksonUtils;
import org.eulerframework.context.support.ClassPathReloadableResourceBundleMessageSource;
import org.eulerframework.web.core.base.controller.PageRender;
import org.eulerframework.web.core.base.controller.ThymeleafPageRender;
import org.eulerframework.web.core.exception.web.SystemWebError;
import org.eulerframework.web.core.exception.web.WebException;
import org.eulerframework.web.servlet.error.ErrorResponseResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.templatemode.TemplateMode;

import java.beans.PropertyEditorSupport;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

@AutoConfiguration(
        before = {
                MessageSourceAutoConfiguration.class,
                ErrorMvcAutoConfiguration.class
        }
)
@EnableConfigurationProperties({
        EulerCopyrightProperties.class,
        EulerCacheProperties.class,
        EulerWebSiteProperties.class,
        EulerWebI18nProperties.class
})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class EulerWebSupportAutoConfiguration {
    private final Logger logger = LoggerFactory.getLogger(EulerWebSupportAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        this.logger.debug("Create ObjectMapper use JacksonUtils");
        return JacksonUtils.getDefaultObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean(PageRender.class)
    @ConditionalOnClass(TemplateMode.class)
    public PageRender pageRender() {
        this.logger.debug("Create default PageRender");
        return new ThymeleafPageRender();
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
        if (!CollectionUtils.isEmpty(properties.getBasename())) {
            messageSource.setBasenames(properties.getBasename().toArray(new String[0]));
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
            EulerCacheProperties eulerCacheProperties) {
        return new EulerBootPropertySource(configurableEnvironment, multipartProperties, eulerApplicationProperties, eulerCacheProperties);
    }

    @Bean
    @ConditionalOnMissingBean(ErrorResponseResolver.class)
    public ErrorResponseResolver errorResponseResolver() {
        return new DefaultErrorResponseResolver();
    }

    @Bean
    @ConditionalOnBean(PageRender.class)
    @ConditionalOnMissingBean(ErrorViewResolver.class)
    public ErrorViewResolver errorViewResolver(PageRender pageRender, ErrorResponseResolver errorResponseResolver) {
        this.logger.debug("Create default ErrorViewResolver");
        return new DefaultErrorViewResolver(pageRender, errorResponseResolver);
    }

    @ControllerAdvice
    public static class GlobalControllerAdvice {
        private final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);

        private final ErrorAttributes errorAttributes;
        private final ErrorProperties errorProperties;
        private final ErrorResponseResolver errorResponseResolver;

        @Autowired
        public GlobalControllerAdvice(ErrorAttributes errorAttributes, ServerProperties serverProperties, ErrorResponseResolver errorResponseResolver) {
            this.errorAttributes = errorAttributes;
            this.errorProperties = serverProperties.getError();
            this.errorResponseResolver = errorResponseResolver;
        }

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

        /**
         * 用于在程序发生{@link Exception}异常时统一返回错误信息
         */
        @ExceptionHandler(value = Exception.class, produces = MediaType.TEXT_HTML_VALUE)
        public ModelAndView exceptionHtml(Exception e) {
            this.logger.error("Unhandled exception thrown: {}", e.getMessage(), e);
            throw ExceptionUtils.asRuntimeException(e); // fallback to default error page
        }

        /**
         * 用于在程序发生{@link Exception}异常时统一返回错误信息
         */
        @ExceptionHandler(value = Exception.class)
        @ResponseBody
        public ResponseEntity<?> exception(HttpServletRequest request, Exception e) {
            this.logger.error("Unhandled exception thrown: {}", e.getMessage(), e);
            HttpStatus status = getStatus(request, e);
            if (status == HttpStatus.NO_CONTENT) {
                return new ResponseEntity<>(status);
            }

            Map<String, Object> model = getErrorAttributes(request, getErrorAttributeOptions(request, MediaType.ALL));
            model.put("error", SystemWebError.UNDEFINED_ERROR.getReasonPhrase());
            model.put("status", SystemWebError.UNDEFINED_ERROR.value());
            return new ResponseEntity<>(
                    this.errorResponseResolver.resolveErrorResponse(request, status, model),
                    status);
        }

        protected ErrorAttributeOptions getErrorAttributeOptions(HttpServletRequest request, MediaType mediaType) {
            ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
            if (this.errorProperties.isIncludeException()) {
                options = options.including(ErrorAttributeOptions.Include.EXCEPTION);
            }
            if (isIncludeStackTrace(request, mediaType)) {
                options = options.including(ErrorAttributeOptions.Include.STACK_TRACE);
            }
            if (isIncludeMessage(request, mediaType)) {
                options = options.including(ErrorAttributeOptions.Include.MESSAGE);
            }
            if (isIncludeBindingErrors(request, mediaType)) {
                options = options.including(ErrorAttributeOptions.Include.BINDING_ERRORS);
            }
            options = isIncludePath(request, mediaType) ? options.including(ErrorAttributeOptions.Include.PATH) : options.excluding(ErrorAttributeOptions.Include.PATH);
            return options;
        }

        /**
         * Determine if the stacktrace attribute should be included.
         *
         * @param request  the source request
         * @param produces the media type produced (or {@code MediaType.ALL})
         * @return if the stacktrace attribute should be included
         */
        protected boolean isIncludeStackTrace(HttpServletRequest request, MediaType produces) {
            return switch (getErrorProperties().getIncludeStacktrace()) {
                case ALWAYS -> true;
                case ON_PARAM -> getTraceParameter(request);
                case NEVER -> false;
            };
        }

        /**
         * Determine if the message attribute should be included.
         *
         * @param request  the source request
         * @param produces the media type produced (or {@code MediaType.ALL})
         * @return if the message attribute should be included
         */
        protected boolean isIncludeMessage(HttpServletRequest request, MediaType produces) {
            return switch (getErrorProperties().getIncludeMessage()) {
                case ALWAYS -> true;
                case ON_PARAM -> getMessageParameter(request);
                case NEVER -> false;
            };
        }

        /**
         * Determine if the errors attribute should be included.
         *
         * @param request  the source request
         * @param produces the media type produced (or {@code MediaType.ALL})
         * @return if the errors attribute should be included
         */
        protected boolean isIncludeBindingErrors(HttpServletRequest request, MediaType produces) {
            return switch (getErrorProperties().getIncludeBindingErrors()) {
                case ALWAYS -> true;
                case ON_PARAM -> getErrorsParameter(request);
                case NEVER -> false;
            };
        }

        /**
         * Determine if the path attribute should be included.
         *
         * @param request  the source request
         * @param produces the media type produced (or {@code MediaType.ALL})
         * @return if the path attribute should be included
         * @since 3.3.0
         */
        protected boolean isIncludePath(HttpServletRequest request, MediaType produces) {
            return switch (getErrorProperties().getIncludePath()) {
                case ALWAYS -> true;
                case ON_PARAM -> getPathParameter(request);
                case NEVER -> false;
            };
        }

        /**
         * Provide access to the error properties.
         *
         * @return the error properties
         */
        protected ErrorProperties getErrorProperties() {
            return this.errorProperties;
        }

        protected Map<String, Object> getErrorAttributes(HttpServletRequest request, ErrorAttributeOptions options) {
            WebRequest webRequest = new ServletWebRequest(request);
            return this.errorAttributes.getErrorAttributes(webRequest, options);
        }

        /**
         * Returns whether the trace parameter is set.
         *
         * @param request the request
         * @return whether the trace parameter is set
         */
        protected boolean getTraceParameter(HttpServletRequest request) {
            return getBooleanParameter(request, "trace");
        }

        /**
         * Returns whether the message parameter is set.
         *
         * @param request the request
         * @return whether the message parameter is set
         */
        protected boolean getMessageParameter(HttpServletRequest request) {
            return getBooleanParameter(request, "message");
        }

        /**
         * Returns whether the errors parameter is set.
         *
         * @param request the request
         * @return whether the errors parameter is set
         */
        protected boolean getErrorsParameter(HttpServletRequest request) {
            return getBooleanParameter(request, "errors");
        }

        /**
         * Returns whether the path parameter is set.
         *
         * @param request the request
         * @return whether the path parameter is set
         * @since 3.3.0
         */
        protected boolean getPathParameter(HttpServletRequest request) {
            return getBooleanParameter(request, "path");
        }

        protected boolean getBooleanParameter(HttpServletRequest request, String parameterName) {
            String parameter = request.getParameter(parameterName);
            if (parameter == null) {
                return false;
            }
            return !"false".equalsIgnoreCase(parameter);
        }

        protected HttpStatus getStatus(HttpServletRequest request, Exception e) {
            Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
            if (statusCode == null) {
                if (e instanceof WebException webException && webException.getStatus() != null) {
                    return webException.getStatus();
                }

                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (Exception ex) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
    }
}
