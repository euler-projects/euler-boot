package org.eulerframework.boot.autoconfigure.support.web.core;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.common.util.type.TypeUtils;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

public class EulerBootPropertySource implements PropertySource {
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    private static final Map<String, EulerBootPropertiesGetter> CONFIG_VALUE_MAPPING = new HashMap<>();

    private final ConfigurableEnvironment environment;

    public EulerBootPropertySource(
            ConfigurableEnvironment environment,
            EulerApplicationProperties eulerApplicationProperties,
            EulerWebProperties eulerWebProperties,
            EulerCacheProperties eulerCacheProperties) {
        this.environment = environment;

        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, eulerApplicationProperties::getRuntimePath);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, eulerApplicationProperties::getTempPath);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, eulerCacheProperties::getRamCachePoolCleanFreq);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, () -> eulerWebProperties.getI18n().getDefaultLanguage());
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, () -> eulerWebProperties.getI18n().getSupportLanguages());
    }

    @Override
    public Object getProperty(String key) throws PropertyNotFoundException {
        if (CONFIG_VALUE_MAPPING.containsKey(key)) {
            return CONFIG_VALUE_MAPPING.get(key).get();
        }

        if (this.environment.containsProperty(key)) {
            return this.environment.getProperty(key);
        }

        throw new PropertyNotFoundException();
    }

    @Override
    public <T> T getProperty(String key, Class<T> requireType) throws PropertyNotFoundException {
        return TypeUtils.convert(this.getProperty(key), requireType);
    }

    @FunctionalInterface
    private interface EulerBootPropertiesGetter {
        Object get();
    }
}
