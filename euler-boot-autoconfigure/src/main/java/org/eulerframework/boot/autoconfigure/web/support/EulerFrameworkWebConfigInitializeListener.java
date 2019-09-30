package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.common.util.property.PropertyReader;
import org.eulerframework.web.config.WebConfig;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class EulerFrameworkWebConfigInitializeListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        SpringBootPropertySource springBootPropertySource =
                new SpringBootPropertySource(environment);
        WebConfig.setPropertyReader(new PropertyReader(springBootPropertySource));
    }
}
