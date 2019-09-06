package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.boot.autoconfigure.web.EulerApplicationProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.common.util.property.PropertySource;
import org.springframework.core.env.ConfigurableEnvironment;

public class SpringBootPropertySource implements PropertySource {
    private static final String EULER_APPLICATION_RUNTIME_PATH = "core.runtimePath";
    private static final String EULER_APPLICATION_TMP_PATH = "euler.application.tmp-path";

    private final ConfigurableEnvironment environment;
    private final EulerApplicationProperties eulerApplicationProperties;

    public SpringBootPropertySource(ConfigurableEnvironment environment, EulerApplicationProperties eulerApplicationProperties) {
        this.environment = environment;
        this.eulerApplicationProperties = eulerApplicationProperties;
    }

    @Override
    public Object getProperty(String key) throws PropertyNotFoundException {
        if (EULER_APPLICATION_RUNTIME_PATH.equals(key)) {
            return this.eulerApplicationProperties.getRuntimePath();
        } else if (EULER_APPLICATION_TMP_PATH.equals(key)) {
            return this.eulerApplicationProperties.getTmpPath();
        } else {
            String value = this.environment.getProperty(key);
            if (value == null) {
                throw new PropertyNotFoundException();
            } else {
                return value;
            }
        }
    }
}
