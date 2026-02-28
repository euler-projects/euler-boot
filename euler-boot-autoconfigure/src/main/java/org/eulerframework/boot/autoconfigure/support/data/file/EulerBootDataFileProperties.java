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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "euler.data.file")
public class EulerBootDataFileProperties {
    private JdbcStorage jdbcStorage = new JdbcStorage();
    private LocalStorage localStorage = new LocalStorage();

    public JdbcStorage getJdbcStorage() {
        return jdbcStorage;
    }

    public void setJdbcStorage(JdbcStorage jdbcStorage) {
        this.jdbcStorage = jdbcStorage;
    }

    public LocalStorage getLocalStorage() {
        return localStorage;
    }

    public void setLocalStorage(LocalStorage localStorage) {
        this.localStorage = localStorage;
    }

    public static class JdbcStorage {
        private boolean enabled = false;
        private String fileDownloadUrlTemplate;
        private DataSize maxFileSize = DataSize.ofKilobytes(256);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFileDownloadUrlTemplate() {
            return fileDownloadUrlTemplate;
        }

        public void setFileDownloadUrlTemplate(String fileDownloadUrlTemplate) {
            this.fileDownloadUrlTemplate = fileDownloadUrlTemplate;
        }

        public DataSize getMaxFileSize() {
            return maxFileSize;
        }

        public void setMaxFileSize(DataSize maxFileSize) {
            this.maxFileSize = maxFileSize;
        }
    }

    public static class LocalStorage {
        private boolean enabled = false;
        private String baseDir;
        private String fileDownloadUrlTemplate;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFileDownloadUrlTemplate() {
            return fileDownloadUrlTemplate;
        }

        public void setFileDownloadUrlTemplate(String fileDownloadUrlTemplate) {
            this.fileDownloadUrlTemplate = fileDownloadUrlTemplate;
        }

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }
    }
}
