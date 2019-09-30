package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

public class SpringBootPropertySource implements PropertySource {
    private static final Map<String, String> CONFIG_KEY_MAPPING = new HashMap<>();

    static {
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_RUNTIME_PATH, "euler.application.runtime-path");
        CONFIG_KEY_MAPPING.put(WebConfigKey.CORE_TMP_PATH, "euler.application.tmp-path");
    }

    private final ConfigurableEnvironment environment;

    public SpringBootPropertySource(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Object getProperty(String key) throws PropertyNotFoundException {
        final String eulerBootKey = CONFIG_KEY_MAPPING.getOrDefault(key, key);
        final String value = this.environment.getProperty(eulerBootKey);

        if (value == null) {
            throw new PropertyNotFoundException();
        } else {
            return value;
        }
    }
}
