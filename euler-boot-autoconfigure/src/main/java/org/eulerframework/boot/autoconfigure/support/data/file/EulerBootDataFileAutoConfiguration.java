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
import org.eulerframework.data.file.registry.FileIndexRegistry;
import org.eulerframework.data.file.registry.JdbcFileIndexRegistry;
import org.eulerframework.data.file.servlet.LocalStorageFileDownloader;
import org.eulerframework.data.file.servlet.StorageFileDownloaderChain;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@AutoConfiguration
@ConditionalOnClass(FileStorage.class)
@EnableConfigurationProperties(EulerBootDataFileProperties.class)
public class EulerBootDataFileAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FileIndexRegistry.class)
    public FileIndexRegistry fileIndexRegistry(JdbcTemplate jdbcTemplate) {
        return new JdbcFileIndexRegistry(jdbcTemplate);
    }

    @Bean
    public StorageFileDownloaderChain storageFileDownloaderChain() {
        return new StorageFileDownloaderChain();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.data.file.jdbc-storage", name = "enabled", havingValue = "true")
    public static class EulerBootDataJdbcFileStorageConfiguration {
        @Bean
        public JdbcFileStorage jdbcFileStorage(JdbcTemplate jdbcTemplate, FileIndexRegistry fileIndexRegistry, EulerBootDataFileProperties properties) {
            JdbcFileStorage jdbcFileStorage = new JdbcFileStorage(jdbcTemplate,
                    properties.getLocalStorage().getFileDownloadUrlTemplate(),
                    fileIndexRegistry);
            jdbcFileStorage.setMaxFileSize(properties.getJdbcStorage().getMaxFileSize());
            return jdbcFileStorage;
        }

        @Bean(name = "jdbcStorageFileDownloader")
        public LocalStorageFileDownloader jdbcStorageFileDownloader(JdbcFileStorage jdbcFileStorage, StorageFileDownloaderChain storageFileDownloaderChain) {
            LocalStorageFileDownloader jdbcStorageFileDownloader = new LocalStorageFileDownloader(jdbcFileStorage);
            storageFileDownloaderChain.add(jdbcStorageFileDownloader);
            return jdbcStorageFileDownloader;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "euler.data.file.local-storage", name = "enabled", havingValue = "true")
    public static class EulerBootDataLocalFileStorageConfiguration {
        @Bean
        public LocalFileStorage localFileStorage(JdbcTemplate jdbcTemplate, FileIndexRegistry fileIndexRegistry, EulerBootDataFileProperties properties) {
            return new LocalFileStorage(jdbcTemplate,
                    properties.getLocalStorage().getFileDownloadUrlTemplate(),
                    properties.getLocalStorage().getBaseDir(),
                    fileIndexRegistry);
        }

        @Bean(name = "localStorageFileDownloader")
        public LocalStorageFileDownloader localStorageFileDownloader(LocalFileStorage localFileStorage, StorageFileDownloaderChain storageFileDownloaderChain) {
            LocalStorageFileDownloader localStorageFileDownloader = new LocalStorageFileDownloader(localFileStorage);
            storageFileDownloaderChain.add(localStorageFileDownloader);
            return localStorageFileDownloader;
        }
    }
}
