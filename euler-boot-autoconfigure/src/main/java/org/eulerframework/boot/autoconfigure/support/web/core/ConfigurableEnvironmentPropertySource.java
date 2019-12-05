package org.eulerframework.boot.autoconfigure.support.web.core;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.common.util.type.TypeUtils;
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
    private static final Map<String, Object> CONFIG_DEFAULT_VALUE_MAPPING = new HashMap<>();

    static {
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_APPLICATION_NAME, SPRING_APPLICATION_NAME);
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, "euler.application.runtime-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, "euler.application.temp-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, "euler.cache.ram-cache-pool-clean-freq");
        CONFIG_KEY_MAPPING.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, "euler.web.i18n.default-language");
        CONFIG_KEY_MAPPING.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, "euler.web.i18n.support-languages");
    }

    private final ConfigurableEnvironment environment;

    public ConfigurableEnvironmentPropertySource(
            ConfigurableEnvironment environment,
            EulerApplicationProperties eulerApplicationProperties,
            EulerWebProperties eulerWebProperties,
            EulerCacheProperties eulerCacheProperties) {
        this.environment = environment;

        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, eulerApplicationProperties.getRuntimePath());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, eulerApplicationProperties.getTempPath());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, eulerCacheProperties.getRamCachePoolCleanFreq());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, eulerWebProperties.getI18n().getDefaultLanguage());
        CONFIG_DEFAULT_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, eulerWebProperties.getI18n().getSupportLanguages());
    }

    @Override
    public Object getProperty(String key) throws PropertyNotFoundException {
        final String eulerBootKey = CONFIG_KEY_MAPPING.getOrDefault(key, key);
        final String value = this.environment.getProperty(eulerBootKey);

        if (value == null) {
            if(CONFIG_DEFAULT_VALUE_MAPPING.containsKey(key)) {
                return CONFIG_DEFAULT_VALUE_MAPPING.get(key);
            } else  {
                throw new PropertyNotFoundException();
            }
        } else  {
            return value;
        }
    }

    @Override
    public <T> T getProperty(String key, Class<T> requireType) throws PropertyNotFoundException {
        return TypeUtils.convert(this.getProperty(key), requireType);
    }

    private interface ValueMapper {
        Object map(String value);
    }
}
