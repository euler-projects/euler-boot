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
package org.eulerframework.boot.autoconfigure.support.web.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cglib.core.Local;

import java.time.Duration;
import java.util.Locale;

@ConfigurationProperties(prefix = "euler.web")
public class EulerWebProperties {
    private I18n i18n = new I18n();

    public I18n getI18n() {
        return i18n;
    }

    public void setI18n(I18n i18n) {
        this.i18n = i18n;
    }

    public static class I18n {
        private String resourcePath = "classpath*:language/**/*";
        private Locale defaultLanguage = Locale.CHINA;
        private Locale[] supportLanguages = new Locale[]{Locale.CHINA, Locale.US};

        public String getResourcePath() {
            return resourcePath;
        }

        public void setResourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
        }

        public Locale getDefaultLanguage() {
            return defaultLanguage;
        }

        public void setDefaultLanguage(Locale defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
        }

        public Locale[] getSupportLanguages() {
            return supportLanguages;
        }

        public void setSupportLanguages(Locale[] supportLanguages) {
            this.supportLanguages = supportLanguages;
        }
    }
}
