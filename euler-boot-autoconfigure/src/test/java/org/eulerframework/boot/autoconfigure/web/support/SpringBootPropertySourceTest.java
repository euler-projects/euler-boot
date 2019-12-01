package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.SpringBootPropertySource;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.junit.Assert;
import org.junit.Test;

public class SpringBootPropertySourceTest {

    @Test
    public void getISO8601StyleDurationProperty() throws PropertyNotFoundException {
        SpringBootPropertySource springBootPropertySource = new SpringBootPropertySource(SingleValueConfigurableEnvironment.of("PT2M1.01S"), new EulerApplicationProperties(), new EulerCacheProperties());

        Assert.assertEquals(Long.class, springBootPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ).getClass());
        Assert.assertEquals(2L * 60 * 1000 + 1010, springBootPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ));
    }

    @Test
    public void getSimpleStyleDurationProperty() throws PropertyNotFoundException {
        SpringBootPropertySource springBootPropertySource = new SpringBootPropertySource(SingleValueConfigurableEnvironment.of("1s"), new EulerApplicationProperties(), new EulerCacheProperties());

        Assert.assertEquals(Long.class, springBootPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ).getClass());
        Assert.assertEquals(1000L, springBootPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ));
    }

}
