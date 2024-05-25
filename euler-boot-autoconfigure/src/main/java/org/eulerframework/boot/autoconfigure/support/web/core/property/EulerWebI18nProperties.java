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
package org.eulerframework.boot.autoconfigure.support.web.core.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Locale;

@ConfigurationProperties(prefix = "euler.web.i18n")
public class EulerWebI18nProperties {
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
