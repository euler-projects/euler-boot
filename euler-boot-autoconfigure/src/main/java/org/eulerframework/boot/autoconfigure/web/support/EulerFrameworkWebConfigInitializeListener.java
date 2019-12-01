package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.boot.autoconfigure.web.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.web.EulerCacheProperties;
import org.eulerframework.common.util.property.PropertyReader;
import org.eulerframework.web.config.WebConfig;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;

public class EulerFrameworkWebConfigInitializeListener implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        SpringBootPropertySource springBootPropertySource = event.getApplicationContext().getBean(SpringBootPropertySource.class);
        WebConfig.setPropertyReader(new PropertyReader(springBootPropertySource));
    }
}
