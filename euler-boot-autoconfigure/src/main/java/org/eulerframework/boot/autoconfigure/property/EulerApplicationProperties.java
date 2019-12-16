/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.autoconfigure.property;

import org.eulerframework.boot.autoconfigure.support.web.core.EulerBootPropertySource;
import org.eulerframework.common.util.CommonUtils;
import org.eulerframework.common.util.StringUtils;
import org.eulerframework.web.config.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.ConfigurableEnvironment;

import java.nio.file.FileSystemException;

@ConfigurationProperties(prefix = "euler.application")
public class EulerApplicationProperties implements InitializingBean {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String DEFAULT_APPLICATION_NAME = "euler-boot";
    private static final String DEFAULT_RUNTIME_PATH_PREFIX = "/var/run";
    private static final String DEFAULT_TEMP_PATH_PREFIX = "/var/tmp";

    @Autowired
    private ConfigurableEnvironment environment;

    private String runtimePath;
    private String tempPath;

    public String getRuntimePath() {
        return runtimePath;
    }

    public void setRuntimePath(String runtimePath) {
        this.runtimePath = CommonUtils.convertDirToUnixFormat(runtimePath, false);
    }

    public String getTempPath() {
        return tempPath;
    }

    public void setTempPath(String tempPath) {
        this.tempPath = CommonUtils.convertDirToUnixFormat(tempPath, false);
    }

    @Override
    public void afterPropertiesSet() throws FileSystemException {
        this.runtimePath = ConfigUtils.handleApplicationPath(
                this.runtimePath,
                () -> {
                    String applicationName = this.environment.getProperty(EulerBootPropertySource.SPRING_APPLICATION_NAME);
                    return DEFAULT_RUNTIME_PATH_PREFIX + "/" + (StringUtils.hasText(applicationName) ? applicationName : DEFAULT_APPLICATION_NAME);
                },
                "euler.application.runtime-path",
                true);

        this.tempPath = ConfigUtils.handleApplicationPath(
                this.tempPath,
                () -> {
                    String applicationName = this.environment.getProperty(EulerBootPropertySource.SPRING_APPLICATION_NAME);
                    return DEFAULT_TEMP_PATH_PREFIX + "/" + (StringUtils.hasText(applicationName) ? applicationName : DEFAULT_APPLICATION_NAME);
                },
                "euler.application.temp-path",
                true);
    }
}
