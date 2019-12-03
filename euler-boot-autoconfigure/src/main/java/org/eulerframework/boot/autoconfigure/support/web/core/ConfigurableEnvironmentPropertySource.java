package org.eulerframework.boot.autoconfigure.support.web.core;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.common.util.property.converter.RawTypeConverterUtils;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ConfigurableEnvironmentPropertySource implements PropertySource {
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    private static final Map<String, String> CONFIG_KEY_MAPPING = new HashMap<>();
    private static final Map<String, ValueMapper> CONFIG_VALUE_MAPPER = new HashMap<>();
    private static final Map<String, Object> CONFIG_DEFAULT_VALUE_MAPPING = new HashMap<>();

    private static final StringToDurationConverter STRING_TO_DURATION_CONVERTER = new StringToDurationConverter();

    private static final TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
    private static final TypeDescriptor DURATION_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Duration.class);
    private static final TypeDescriptor LOCALE_ARRAY_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Locale[].class);

    static {
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_APPLICATION_NAME, SPRING_APPLICATION_NAME);
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, "euler.application.runtime-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, "euler.application.temp-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, "euler.cache.ram-cache-pool-clean-freq");
        CONFIG_KEY_MAPPING.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, "euler.web.i18n.default-language");
        CONFIG_KEY_MAPPING.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, "euler.web.i18n.support-languages");
    }

    private final ConfigurableEnvironment environment;
    private final ConversionService conversionService;
    private final SimpleTypeConverter simpleTypeConverter = new SimpleTypeConverter();

    public ConfigurableEnvironmentPropertySource(
            ConversionService conversionService,
            ConfigurableEnvironment environment,
            EulerApplicationProperties eulerApplicationProperties,
            EulerWebProperties eulerWebProperties,
            EulerCacheProperties eulerCacheProperties) {
        this.environment = environment;
        this.conversionService = conversionService;

        CONFIG_VALUE_MAPPER.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, value -> {
            Duration duration = (Duration) STRING_TO_DURATION_CONVERTER.convert(value, STRING_TYPE_DESCRIPTOR, DURATION_TYPE_DESCRIPTOR);
            return duration == null ? null : duration.toMillis();
        });
        CONFIG_VALUE_MAPPER.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, value -> this.simpleTypeConverter.convertIfNecessary(value, Locale.class));
        CONFIG_VALUE_MAPPER.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, value -> {
            DelimitedStringToArrayConverter delimitedStringToArrayConverter = new DelimitedStringToArrayConverter(this.conversionService);
            return delimitedStringToArrayConverter.convert(value, STRING_TYPE_DESCRIPTOR, LOCALE_ARRAY_TYPE_DESCRIPTOR);
        });

        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, eulerApplicationProperties.getRuntimePath());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, eulerApplicationProperties.getTempPath());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, eulerCacheProperties.getRamCachePoolCleanFreq().toMillis());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, eulerWebProperties.getI18n().getDefaultLanguage());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, eulerWebProperties.getI18n().getSupportLanguages());
    }

    @Override
    public Object getProperty(String key) throws PropertyNotFoundException {
        final String eulerBootKey = CONFIG_KEY_MAPPING.getOrDefault(key, key);
        final String value = this.environment.getProperty(eulerBootKey);

        if (value == null) {
            Object defaultValue = CONFIG_DEFAULT_VALUE_MAPPING.get(key);

            if (defaultValue == null) {
                throw new PropertyNotFoundException();
            }

            return defaultValue;
        }

        ValueMapper valueMapper = CONFIG_VALUE_MAPPER.get(key);

        if (valueMapper == null) {
            return value;
        }

        return valueMapper.map(value);
    }

    @Override
    public <T> T getProperty(String key, Class<T> requireType) throws PropertyNotFoundException {
        return RawTypeConverterUtils.convert(this.getProperty(key), requireType);
    }

    private interface ValueMapper {
        Object map(String value);
    }
}
