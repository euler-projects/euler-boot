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

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.springframework.boot.context.properties.bind.DataObjectPropertyName;
import org.springframework.boot.servlet.autoconfigure.MultipartProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

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
            EulerCacheProperties eulerCacheProperties) {
        this.environment = environment;

        // [core]
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_APPLICATION_NAME, () -> this.environment.getProperty(SPRING_APPLICATION_NAME));
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, eulerApplicationProperties::getRuntimePath);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_TEMP_PATH, eulerApplicationProperties::getTempPath);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_ADDITIONAL_CONF_PATH, () -> this.environment.getProperty("spring.config.additional-location"));

        // [core.cache]
        CONFIG_VALUE_MAPPING.put(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, eulerCacheProperties::getRamCachePoolCleanFreq);

        // [web.multiPart]
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_FILE_SIZE_THRESHOLD, multipartProperties::getFileSizeThreshold);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_LOCATION, multipartProperties::getLocation);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_MAX_FILE_SIZE, multipartProperties::getMaxFileSize);
        CONFIG_VALUE_MAPPING.put(WebConfigKey.WEB_MULTIPART_MAX_REQUEST_SIZE, multipartProperties::getMaxRequestSize);
    }

    @Override
    public Object getProperty(String key) throws PropertyNotFoundException {
        key = this.renderKey(key);
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
        key = this.renderKey(key);
        if (CONFIG_VALUE_MAPPING.containsKey(key)) {
            return (T) CONFIG_VALUE_MAPPING.get(key).get();
        }

        if (this.environment.containsProperty(key)) {
            return this.environment.getProperty(key, requireType);
        }

        throw new PropertyNotFoundException();
    }

    private String renderKey(String key) {
        Assert.hasText(key, "key must not be empty");
        String dashedFormKey = DataObjectPropertyName.toDashedForm(key);
        if(!dashedFormKey.startsWith("euler.")) {
            dashedFormKey = "euler." + dashedFormKey;
        }
        return dashedFormKey;
    }

    @FunctionalInterface
    private interface EulerBootPropertiesGetter {
        Object get();
    }
}
