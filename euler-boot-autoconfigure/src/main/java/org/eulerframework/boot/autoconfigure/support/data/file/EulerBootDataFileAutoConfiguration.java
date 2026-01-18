/*
 * Copyright 2013-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eulerframework.boot.autoconfigure.support.data.file;

import org.eulerframework.data.file.FileStorage;
import org.eulerframework.data.file.JdbcFileStorage;
import org.eulerframework.data.file.LocalFileStorage;
import org.eulerframework.data.file.servlet.JdbcStorageFileDownloader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnClass(FileStorage.class)
@EnableConfigurationProperties(EulerBootDataFileProperties.class)
public class EulerBootDataFileAutoConfiguration {
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.data.file.jdbc-storage", name = "enabled", havingValue = "true")
    public static class EulerBootDataJdbcFileStorageConfiguration {
        @Bean
        public JdbcFileStorage jdbcFileStorage(JdbcTemplate jdbcTemplate, EulerBootDataFileProperties properties) {
            JdbcFileStorage jdbcFileStorage = new JdbcFileStorage(jdbcTemplate,
                    properties.getLocalStorage().getFileDownloadUrlTemplate());
            jdbcFileStorage.setMaxFileSize(properties.getJdbcStorage().getMaxFileSize());
            return jdbcFileStorage;
        }

        @Bean
        public JdbcStorageFileDownloader jdbcStorageFileDownloader(JdbcFileStorage jdbcFileStorage) {
            return new JdbcStorageFileDownloader(jdbcFileStorage);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.data.file.local-storage", name = "enabled", havingValue = "true")
    public static class EulerBootDataLocalFileStorageConfiguration {
        @Bean
        public LocalFileStorage localFileStorage(JdbcTemplate jdbcTemplate, EulerBootDataFileProperties properties) {
            return new LocalFileStorage(jdbcTemplate,
                    properties.getLocalStorage().getFileDownloadUrlTemplate(),
                    properties.getLocalStorage().getBaseDir());
        }
    }
}
