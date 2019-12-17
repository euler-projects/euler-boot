package org.eulerframework.boot.autoconfigure.support.web.core;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebI18nProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebSiteProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

public class EulerBootPropertySource implements PropertySource {
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";

    private static final Map<String, EulerBootPropertiesGetter> CONFIG_VALUE_MAPPING = new HashMap<>();

    private final ConfigurableEnvironment environment;

    public EulerBootPropertySource(
            ConfigurableEnvironment environment,
            MultipartProperties multipartProperties,
            EulerApplicationProperties eulerApplicationProperties,
            EulerWebSiteProperties eulerWebSiteProperties,
            EulerWebI18nProperties eulerWebI18nProperties,
            EulerCacheProperties eulerCacheProperties) {
        this.environment = environment;

        // [core]
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_APPLICATION_NAME, () -> this.environment.getProperty(SPRING_APPLICATION_NAME));
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, eulerApplicationProperties::getRuntimePath);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, eulerApplicationProperties::getTempPath);

        // [core.cache]
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, eulerCacheProperties::getRamCachePoolCleanFreq);

        // [web]
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_SITE_URL, eulerWebSiteProperties::getUrl);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_DEFAULT, eulerWebI18nProperties::getDefaultLanguage);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, eulerWebI18nProperties::getSupportLanguages);

        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_FILE_SIZE_THRESHOLD, multipartProperties::getFileSizeThreshold);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_LOCATION, multipartProperties::getLocation);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_MAX_FILE_SIZE, multipartProperties::getMaxFileSize);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_MAX_REQUEST_SIZE, multipartProperties::getMaxRequestSize);
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
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> requireType) throws PropertyNotFoundException {
        if (CONFIG_VALUE_MAPPING.containsKey(key)) {
            return (T) CONFIG_VALUE_MAPPING.get(key).get();
        }

        if (this.environment.containsProperty(key)) {
            return this.environment.getProperty(key, requireType);
        }

        throw new PropertyNotFoundException();
    }

    @FunctionalInterface
    private interface EulerBootPropertiesGetter {
        Object get();
    }
}
