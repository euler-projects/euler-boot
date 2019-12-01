package org.eulerframework.boot.autoconfigure.web.support;

import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MissingRequiredPropertiesException;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.Profiles;

import java.util.Map;

class SingleValueConfigurableEnvironment {
    static ConfigurableEnvironment of(String value) {
        return new ConfigurableEnvironment() {
            @Override
            public String getProperty(String key) {
                return value;
            }

            @Override
            public boolean containsProperty(String key) {
                return false;
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                return null;
            }

            @Override
            public <T> T getProperty(String key, Class<T> targetType) {
                return null;
            }

            @Override
            public <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
                return null;
            }

            @Override
            public String getRequiredProperty(String key) throws IllegalStateException {
                return null;
            }

            @Override
            public <T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException {
                return null;
            }

            @Override
            public String resolvePlaceholders(String text) {
                return null;
            }

            @Override
            public String resolveRequiredPlaceholders(String text) throws IllegalArgumentException {
                return null;
            }

            @Override
            public String[] getActiveProfiles() {
                return new String[0];
            }

            @Override
            public String[] getDefaultProfiles() {
                return new String[0];
            }

            @Override
            public boolean acceptsProfiles(String... profiles) {
                return false;
            }

            @Override
            public boolean acceptsProfiles(Profiles profiles) {
                return false;
            }

            @Override
            public ConfigurableConversionService getConversionService() {
                return null;
            }

            @Override
            public void setConversionService(ConfigurableConversionService conversionService) {

            }

            @Override
            public void setPlaceholderPrefix(String placeholderPrefix) {

            }

            @Override
            public void setPlaceholderSuffix(String placeholderSuffix) {

            }

            @Override
            public void setValueSeparator(String valueSeparator) {

            }

            @Override
            public void setIgnoreUnresolvableNestedPlaceholders(boolean ignoreUnresolvableNestedPlaceholders) {

            }

            @Override
            public void setRequiredProperties(String... requiredProperties) {

            }

            @Override
            public void validateRequiredProperties() throws MissingRequiredPropertiesException {

            }

            @Override
            public void setActiveProfiles(String... profiles) {

            }

            @Override
            public void addActiveProfile(String profile) {

            }

            @Override
            public void setDefaultProfiles(String... profiles) {

            }

            @Override
            public MutablePropertySources getPropertySources() {
                return null;
            }

            @Override
            public Map<String, Object> getSystemProperties() {
                return null;
            }

            @Override
            public Map<String, Object> getSystemEnvironment() {
                return null;
            }

            @Override
            public void merge(ConfigurableEnvironment parent) {

            }
        };
    }
}
