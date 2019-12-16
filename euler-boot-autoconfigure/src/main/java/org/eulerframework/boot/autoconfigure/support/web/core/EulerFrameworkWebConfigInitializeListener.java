package org.eulerframework.boot.autoconfigure.support.web.core;

import org.eulerframework.common.util.property.PropertyReader;
import org.eulerframework.web.config.WebConfig;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

public class EulerFrameworkWebConfigInitializeListener implements ApplicationListener<ApplicationStartedEvent> {

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        EulerBootPropertySource eulerBootPropertySource = event.getApplicationContext().getBean(EulerBootPropertySource.class);
        WebConfig.setPropertyReader(new PropertyReader(eulerBootPropertySource));
    }
}
