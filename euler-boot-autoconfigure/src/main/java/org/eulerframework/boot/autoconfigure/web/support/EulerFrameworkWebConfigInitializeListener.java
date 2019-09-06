package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.boot.autoconfigure.web.EulerApplicationProperties;
import org.eulerframework.common.util.property.PropertyReader;
import org.eulerframework.web.config.WebConfig;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.core.env.ConfigurableEnvironment;

public class EulerFrameworkWebConfigInitializeListener implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        ConfigurableEnvironment environment = event.getApplicationContext().getBean(ConfigurableEnvironment.class);
        EulerApplicationProperties eulerApplicationProperties = event.getApplicationContext().getBean(EulerApplicationProperties.class);
        SpringBootPropertySource springBootPropertySource =
                new SpringBootPropertySource(environment, eulerApplicationProperties);
        WebConfig.setPropertyReader(new PropertyReader(springBootPropertySource));
    }
}
