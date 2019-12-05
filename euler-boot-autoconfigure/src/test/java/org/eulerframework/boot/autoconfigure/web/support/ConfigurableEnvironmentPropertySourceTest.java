package org.eulerframework.boot.autoconfigure.web.support;

import org.eulerframework.boot.autoconfigure.property.EulerApplicationProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.ConfigurableEnvironmentPropertySource;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerCacheProperties;
import org.eulerframework.boot.autoconfigure.support.web.core.property.EulerWebProperties;
import org.eulerframework.common.util.property.PropertyNotFoundException;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.cglib.core.Local;

import java.time.Duration;
import java.util.Locale;

public class ConfigurableEnvironmentPropertySourceTest {

    @Test
    public void getISO8601StyleDurationProperty() throws PropertyNotFoundException {
        ConfigurableEnvironmentPropertySource configurableEnvironmentPropertySource = new ConfigurableEnvironmentPropertySource(SingleValueConfigurableEnvironment.of("PT2M1.01S"), new EulerApplicationProperties(), new EulerWebProperties(), new EulerCacheProperties());

        Assert.assertEquals(Duration.class, configurableEnvironmentPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, Duration.class).getClass());
        Assert.assertEquals(Duration.parse("PT2M1.01S"), configurableEnvironmentPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, Duration.class));
    }

    @Test
    public void getSimpleStyleDurationProperty() throws PropertyNotFoundException {
        ConfigurableEnvironmentPropertySource configurableEnvironmentPropertySource = new ConfigurableEnvironmentPropertySource(SingleValueConfigurableEnvironment.of("1s"), new EulerApplicationProperties(), new EulerWebProperties(), new EulerCacheProperties());

        Assert.assertEquals(Duration.class, configurableEnvironmentPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, Duration.class).getClass());
        Assert.assertEquals(Duration.ofSeconds(1), configurableEnvironmentPropertySource.getProperty(WebConfigKey.CORE_CACHE_RAM_CACHE_POOL_CLEAN_FREQ, Duration.class));
    }

    @Test
    public void getLocale() throws PropertyNotFoundException {
        ConfigurableEnvironmentPropertySource configurableEnvironmentPropertySource = new ConfigurableEnvironmentPropertySource(SingleValueConfigurableEnvironment.of("en_GB"), new EulerApplicationProperties(), new EulerWebProperties(), new EulerCacheProperties());

        Assert.assertEquals(Locale.class, configurableEnvironmentPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_DEFAULT, Locale.class).getClass());
        Assert.assertEquals(Locale.UK, configurableEnvironmentPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_DEFAULT, Locale.class));
    }

    @Test
    public void getLocaleArray() throws PropertyNotFoundException {
        ConfigurableEnvironmentPropertySource configurableEnvironmentPropertySource = new ConfigurableEnvironmentPropertySource(SingleValueConfigurableEnvironment.of("zh_CN, en_US, en_GB"), new EulerApplicationProperties(), new EulerWebProperties(), new EulerCacheProperties());

        Assert.assertEquals(Locale[].class, configurableEnvironmentPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, Locale[].class).getClass());
        Assert.assertArrayEquals(new Locale[]{Locale.CHINA, Locale.US, Locale.UK}, configurableEnvironmentPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, Locale[].class));
    }

    @Test
    public void getDefaultLocaleArray() throws PropertyNotFoundException {
        ConfigurableEnvironmentPropertySource configurableEnvironmentPropertySource = new ConfigurableEnvironmentPropertySource(SingleValueConfigurableEnvironment.of(null), new EulerApplicationProperties(), new EulerWebProperties(), new EulerCacheProperties());

        Assert.assertEquals(Locale[].class, configurableEnvironmentPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, Locale[].class).getClass());
        Assert.assertArrayEquals(new Locale[]{Locale.CHINA, Locale.US}, configurableEnvironmentPropertySource.getProperty(WebConfigKey.WEB_LANGUAGE_SUPPORT_LANGUAGES, Locale[].class));
    }

}
