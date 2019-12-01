package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.boot.autoconfigure.web.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.web.EulerCacheProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.env.ConfigurableEnvironment;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class SpringBootPropertySource implements PropertySource {
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    private static final Map<String, String> CONFIG_KEY_MAPPING = new HashMap<>();
    private static final Map<String, ValueMapper> CONFIG_VALUE_MAPPER = new HashMap<>();
    private static final Map<String, Object> CONFIG_DEFAULT_VALUE_MAPPING = new HashMap<>();

    private static final StringToDurationConverter STRING_TO_DURATION_CONVERTER = new StringToDurationConverter();

    static {
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_APPLICATION_NAME, SPRING_APPLICATION_NAME);
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, "euler.application.runtime-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, "euler.application.temp-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, "euler.cache.ram-cache-pool-clean-freq");

        CONFIG_VALUE_MAPPER.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, value -> {
            Duration duration = (Duration) STRING_TO_DURATION_CONVERTER.convert(value, TypeDescriptor.valueOf(String.class), TypeDescriptor.valueOf(Duration.class));
            return duration == null ? null : duration.toMillis();
        });
    }

    private final ConfigurableEnvironment environment;

    public SpringBootPropertySource(
            ConfigurableEnvironment environment,
            EulerApplicationProperties eulerApplicationProperties,
            EulerCacheProperties eulerCacheProperties) {
        this.environment = environment;

        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, eulerApplicationProperties.getRuntimePath());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, eulerApplicationProperties.getTempPath());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, eulerCacheProperties.getRamCachePoolCleanFreq().toMillis());
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

    private interface ValueMapper {
        Object map(String value);
    }
}
