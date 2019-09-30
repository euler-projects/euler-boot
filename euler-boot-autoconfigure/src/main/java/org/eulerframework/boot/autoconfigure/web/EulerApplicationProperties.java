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
package org.eulerframework.boot.autoconfigure.web;

import org.eulerframework.common.util.CommonUtils;
import org.eulerframework.common.util.StringUtils;
import org.eulerframework.web.config.ConfigUtils;
import org.eulerframework.web.config.WebConfig;
import org.eulerframework.web.config.WebConfig.WebConfigDefault;
import org.eulerframework.web.config.WebConfig.WebConfigKey;
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

    private static final String SPRING_APPLICATION_NAME = WebConfigKey.SPRING_APPLICATION_NAME;
    private static final String DEFAULT_APPLICATION_NAME = WebConfigDefault.DEFAULT_APPLICATION_NAME;

    private static final String DEFAULT_RUNTIME_PATH_PREFIX = WebConfigDefault.DEFAULT_RUNTIME_PATH_PREFIX;
    private static final String DEFAULT_TEMP_PATH_PREFIX = WebConfigDefault.DEFAULT_TEMP_PATH_PREFIX;

    @Autowired
    private ConfigurableEnvironment environment;

    private String runtimePath;
    private String tmpPath;

    public String getRuntimePath() {
        return runtimePath;
    }

    public void setRuntimePath(String runtimePath) {
        this.runtimePath = CommonUtils.convertDirToUnixFormat(runtimePath, false);
    }

    public String getTmpPath() {
        return tmpPath;
    }

    public void setTmpPath(String tmpPath) {
        this.tmpPath = CommonUtils.convertDirToUnixFormat(tmpPath, false);
    }

    @Override
    public void afterPropertiesSet() throws FileSystemException {
        this.runtimePath = ConfigUtils.handleApplicationPath(
                this.runtimePath,
                () -> {
                    String applicationName = this.environment.getProperty(SPRING_APPLICATION_NAME);
                    return DEFAULT_RUNTIME_PATH_PREFIX + "/" + (StringUtils.hasText(applicationName) ? applicationName : DEFAULT_APPLICATION_NAME);
                },
                "euler.application.runtime-path");

        this.tmpPath = ConfigUtils.handleApplicationPath(
                this.tmpPath,
                () -> {
                    String applicationName = this.environment.getProperty(SPRING_APPLICATION_NAME);
                    return DEFAULT_TEMP_PATH_PREFIX + "/" + (StringUtils.hasText(applicationName) ? applicationName : DEFAULT_APPLICATION_NAME);
                },
                "euler.application.tmp-path");
    }
}
