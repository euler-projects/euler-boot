package org.eulerframework.boot.autoconfigure.support.web.core;

import org.eulerframework.common.util.property.PropertyReader;
import org.eulerframework.constant.EulerSysAttributes;
import org.eulerframework.web.config.SystemProperties;
import org.eulerframework.web.config.WebConfig;
import org.eulerframework.web.core.cookie.LocaleCookies;
import org.eulerframework.web.util.ServletContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.util.Assert;

import jakarta.servlet.ServletContext;

public class EulerFrameworkInitializeListener implements ApplicationListener<ServletWebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        EulerFrameworkInitializerHolder.INSTANCE.onApplicationEvent(event);
    }

    /**
     * {@link EulerFrameworkInitializeListener} 对象在启用 Spring Cloud 时会被实例化多次并触发多次 {@link ServletWebServerInitializedEvent},
     * 用单例和一个 {@link EulerFrameworkInitializer#initialized} 标记临时解决这个问题
     */
    private static class EulerFrameworkInitializerHolder {
        private final static EulerFrameworkInitializer INSTANCE = new EulerFrameworkInitializer();
    }

    private static class EulerFrameworkInitializer implements ApplicationListener<ServletWebServerInitializedEvent> {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private boolean initialized = false;

        @Override
        public void onApplicationEvent(ServletWebServerInitializedEvent event) {

            synchronized (this) {
                if(initialized) {
                    this.logger.warn("Euler Framework has initialized already");
                    return;
                }

                initialized = true;
            }

            this.logger.info("Initializing ServletContextHolder");
            ServletContext servletContext = event.getApplicationContext().getServletContext();
            ServletContextHolder.holdServletContext(servletContext);

            this.logger.info("Initializing Web Config");
            EulerBootPropertySource eulerBootPropertySource = event.getApplicationContext().getBean(EulerBootPropertySource.class);
            WebConfig.setPropertyReader(new PropertyReader(eulerBootPropertySource));

            this.logger.info("Initializing System Attributes");
            Assert.notNull(servletContext, "ServletContext was not found");
            this.initBaseData(servletContext);
        }

        private void initBaseData(ServletContext container) {
            String contextPath = container.getContextPath();
            container.setAttribute(EulerSysAttributes.WEB_URL.value(), WebConfig.getWebUrl());

            container.setAttribute(EulerSysAttributes.CONTEXT_PATH.value(), contextPath);
            container.setAttribute(EulerSysAttributes.ASSETS_PATH.value(), contextPath + WebConfig.getAssetsPath());
            container.setAttribute(EulerSysAttributes.ADMIN_PATH.value(), contextPath + WebConfig.getAdminRootPath());
            container.setAttribute(EulerSysAttributes.AJAX_PATH.value(), contextPath + "/ajax");
            container.setAttribute(EulerSysAttributes.ADMIN_AJAX_PATH.value(), contextPath + WebConfig.getAdminRootPath() + "/ajax");

//        container.setAttribute(EulerSysAttributes.PROJECT_VERSION.value(), EulerWebSupportConfig.getProjectVersion());
//        container.setAttribute(EulerSysAttributes.PROJECT_MODE.value(), EulerWebSupportConfig.getProjectMode());
//        container.setAttribute(EulerSysAttributes.PROJECT_BUILD_TIME.value(), EulerWebSupportConfig.getProjectBuildTime());

            container.setAttribute(EulerSysAttributes.SITE_NAME.value(), WebConfig.getSiteName());
//        container.setAttribute(EulerSysAttributes.COPYRIGHT_HOLDER.value(), EulerWebSupportConfig.getCopyrightHolder());
            container.setAttribute(EulerSysAttributes.ADMIN_DASHBOARD_BRAND_ICON.value(), contextPath + WebConfig.getAdminDashboardBrandIcon());
            container.setAttribute(EulerSysAttributes.ADMIN_DASHBOARD_BRAND_TEXT.value(), WebConfig.getAdminDashboardBrandText());

            container.setAttribute(EulerSysAttributes.FRAMEWORK_VERSION.value(), SystemProperties.frameworkVersion());

            container.setAttribute(EulerSysAttributes.LOCALE_COOKIE_NAME.value(), LocaleCookies.LOCALE.getCookieName());
        }
    }

}
