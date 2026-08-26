package sk.iway.iwcm.system.spring;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;

import net.sourceforge.stripes.controller.StripesFilterIway;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.CharacterEncodingFilter;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.system.context.ContextFilter;

/**
 * Spring Boot 4.x application starter.
 * This class provides the Spring Boot entry point for both embedded server
 * and WAR deployment to external Tomcat 11.
 *
 * For embedded server: ./gradlew bootRun
 * For external Tomcat 11 deployment: ./gradlew bootWar
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    // JPA repositories auto-configuration is managed by BaseSpringConfig
    DataJpaRepositoriesAutoConfiguration.class,
    // Security auto-configuration is managed by SpringSecurityConf
    SecurityAutoConfiguration.class
})
@Import({
    SpringAppInitializer.class,
    WebjetEmbeddedTomcatConfiguration.class,
    SetupApplicationConfiguration.class,
    LicenseRecoveryApplicationConfiguration.class,
    ProductionApplicationConfiguration.class,
    SpringBootStarter.ProductionServletInfrastructureConfiguration.class,
    SpringBootStarter.ProductionServletConfiguration.class
})
public class SpringBootStarter extends SpringBootServletInitializer {

    private static final org.apache.commons.logging.Log BOOTSTRAP_LOG =
        org.apache.commons.logging.LogFactory.getLog(SpringBootStarter.class);

    public static void main(String[] args) {
        Logger.info(SpringBootStarter.class, "=== WebJET CMS starting with Spring Boot 4.x ===");

        runApplication(args);
    }

    /**
     * Support for deploying as WAR to external Tomcat.
     * Extends SpringBootServletInitializer to allow
     * ./gradlew bootWar to produce a deployable WAR.
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return configureApplicationBuilder(application, null);
    }

    /**
     * The updater temporarily deploys a minimal descriptor while replacing the
     * application classes. Starting the full Boot application in that context
     * would initialize WebJET against files that are being updated.
     */
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        if (isUpdaterDeployment(servletContext)) {
            BOOTSTRAP_LOG.info("Skipping Spring Boot initialization for the WebJET updater deployment");
            return;
        }
        super.onStartup(servletContext);
    }

    private boolean isUpdaterDeployment(ServletContext servletContext) {
        ServletRegistration updaterRegistration = servletContext.getServletRegistration("updaterinit");
        return updaterRegistration != null
            && "sk.updater.InitServlet".equals(updaterRegistration.getClassName());
    }

    private static void runApplication(String[] args) {
        runApplication(args, SpringBootStarter::createApplicationBuilder);
    }

    static void runApplication(String[] args,
            Function<WebjetBootstrapMode, SpringApplicationBuilder> applicationFactory) {
        try {
            runApplication(applicationFactory.apply(null), args);
        } catch (RuntimeException ex) {
            if (WebjetLicenseRecoveryRequiredException.isCausedBy(ex) == false) {
                throw ex;
            }

            Logger.warn(SpringBootStarter.class,
                "WebJET production initialization detected an invalid license; rebuilding the context in license recovery mode");
            runApplication(applicationFactory.apply(WebjetBootstrapMode.LICENSE_RECOVERY), args);
        }
    }

    static void runApplication(SpringApplicationBuilder application, String[] args) {
        application.run(args != null ? args : new String[0]);
    }

    private static SpringApplicationBuilder createApplicationBuilder(WebjetBootstrapMode forcedMode) {
        return configureApplicationBuilder(new SpringApplicationBuilder(), forcedMode);
    }

    private static SpringApplicationBuilder configureApplicationBuilder(SpringApplicationBuilder application,
            WebjetBootstrapMode forcedMode) {
        return application
            .sources(SpringBootStarter.class)
            .initializers(forcedMode == null
                ? new WebjetBootstrapApplicationContextInitializer()
                : new WebjetBootstrapApplicationContextInitializer(forcedMode))
            .properties(
                "spring.profiles.default:default",
                "server.servlet.context-path:/",
                "server.tomcat.basedir:."
            );
    }

    /**
     * Servlet infrastructure shared by embedded and external WAR deployments.
     * Values come from the immutable bootstrap snapshot, which is available
     * before Spring creates servlet initializer beans.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.PRODUCTION_VALUE)
    static class ProductionServletInfrastructureConfiguration {

        private static final Set<String> MULTIPART_SERVLET_CLASS_NAMES = Set.of(
            sk.iway.iwcm.components.upload.XhrFileUploadServlet.class.getName(),
            sk.iway.iwcm.admin.upload.AdminUploadServlet.class.getName()
        );

        /**
         * Reject setup-only Spring Security sessions before the production
         * security filter can restore a context persisted by Tomcat.
         */
        @Bean
        public FilterRegistrationBean<PersistedSetupAuthenticationCleanupFilter>
                persistedSetupAuthenticationCleanupFilterRegistration(
                    Environment environment) {
            int securityFilterOrder = environment.getProperty(
                "spring.security.filter.order",
                Integer.class,
                SecurityFilterProperties.DEFAULT_FILTER_ORDER
            );
            if (securityFilterOrder == Ordered.HIGHEST_PRECEDENCE) {
                throw new IllegalStateException(
                    "spring.security.filter.order must leave room for the WebJET setup-session cleanup filter"
                );
            }

            FilterRegistrationBean<PersistedSetupAuthenticationCleanupFilter> registration =
                new FilterRegistrationBean<>();
            registration.setFilter(new PersistedSetupAuthenticationCleanupFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(securityFilterOrder - 1);
            registration.setName("persistedSetupAuthenticationCleanupFilter");
            return registration;
        }

        /**
         * Annotation-discovered servlets in an external WAR are owned by the
         * container, so Boot cannot apply its MultipartConfigElement to them.
         */
        @Bean
        @ConditionalOnWarDeployment
        @ConditionalOnBooleanProperty(name = "spring.servlet.multipart.enabled", matchIfMissing = true)
        public ServletContextInitializer externalWarMultipartServletInitializer(
                MultipartConfigElement multipartConfigElement) {
            return servletContext -> servletContext.getServletRegistrations().values().stream()
                .filter(registration -> MULTIPART_SERVLET_CLASS_NAMES.contains(registration.getClassName()))
                .filter(ServletRegistration.Dynamic.class::isInstance)
                .map(ServletRegistration.Dynamic.class::cast)
                .forEach(registration -> registration.setMultipartConfig(multipartConfigElement));
        }
    }

    /**
     * Servlet and filter registrations used by embedded WebJET only. In a
     * traditional WAR, container metadata (web.xml and servlet annotations)
     * owns these legacy registrations instead of creating a second dynamic copy.
     * The bootstrap property also keeps this infrastructure out of setup mode.
     */
    @Configuration
    @ConditionalOnNotWarDeployment
    @ConditionalOnProperty(name = WebjetBootstrapMode.PROPERTY_NAME, havingValue = WebjetBootstrapMode.PRODUCTION_VALUE)
    static class ProductionServletConfiguration {

        /**
         * Register CharacterEncodingFilter for configured encoding support.
         * This filter sets the character encoding for request/response based on configuration.
         * Must be registered before Spring Security and every other filter that may read
         * request parameters.
         */
        @Bean
        @ConditionalOnBooleanProperty(name = "spring.servlet.encoding.enabled", matchIfMissing = true)
        public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilterRegistration(
                CharacterEncodingFilter webjetCharacterEncodingFilter) {
            FilterRegistrationBean<CharacterEncodingFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(webjetCharacterEncodingFilter);
            registration.addUrlPatterns("/*");
            registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Must be first (before all other filters)
            registration.setName("SpringEncodingFilter");
            Logger.info(SpringBootStarter.class, "Registered WebJET CharacterEncodingFilter");
            return registration;
        }

        /**
         * Register ContextFilter for embedded Spring Boot mode.
         * This filter handles context path routing and was previously
         * configured in web.xml for external Tomcat deployments.
         */
        @Bean
        public FilterRegistrationBean<ContextFilter> contextFilterRegistration() {
            FilterRegistrationBean<ContextFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new ContextFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(1);
            registration.setName("ContextFilter");
            return registration;
        }

        /**
         * Register StripesFilter for embedded Spring Boot mode.
         * Required by legacy CSRF/token code paths that access Stripes configuration.
         */
        @Bean
        public FilterRegistrationBean<StripesFilterIway> stripesFilterRegistration() {
            FilterRegistrationBean<StripesFilterIway> registration = new FilterRegistrationBean<>();
            registration.setFilter(new StripesFilterIway());
            registration.addUrlPatterns("/*");
            registration.addServletNames("StripesDispatcher");
            registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
            registration.setOrder(3);
            registration.setName("StripesFilter");
            return registration;
        }

        /**
         * Register Virtual Path Filter for embedded Spring Boot mode.
         */
        @Bean
        public FilterRegistrationBean<PathFilter> virtualPathFilterRegistration() {
            FilterRegistrationBean<PathFilter> registration = new FilterRegistrationBean<>();
            registration.setFilter(new PathFilter());
            registration.addUrlPatterns("/*");
            registration.setOrder(4);
            registration.setName("Virtual Path Filter");
            return registration;
        }

        /**
         * Register RequestContextListener for embedded Spring Boot mode.
         * Required for RequestContextHolder.getRequestAttributes() to work correctly.
         * Used by multiple components that need access to the current request.
         */
        @Bean
        public ServletListenerRegistrationBean<RequestContextListener> requestContextListenerRegistration() {
            Logger.info(SpringBootStarter.class, "Registered RequestContextListener");
            return new ServletListenerRegistrationBean<>(new RequestContextListener());
        }

        /**
         * Mirror the session cleanup listener declared in web.xml for embedded
         * deployments, where the deployment descriptor is not processed.
         */
        @Bean
        public ServletListenerRegistrationBean<sk.iway.iwcm.stat.SessionListener> sessionListenerRegistration() {
            return new ServletListenerRegistrationBean<>(new sk.iway.iwcm.stat.SessionListener());
        }

        /**
         * Keep the legacy servlet lifecycle callback in embedded deployments.
         * It intentionally has no URL mapping; load-on-startup ensures that the
         * container invokes destroy() during a graceful shutdown.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.InitServlet> iwcmInitServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.InitServlet> registration =
                new ServletRegistrationBean<>(new sk.iway.iwcm.InitServlet(), false);
            registration.setName("iwcminit");
            registration.setLoadOnStartup(1);
            return registration;
        }

        /**
         * Register ShowDoc servlet for /showdoc.do paths.
         * Migrated from legacy servlet-mapping in web.xml to Spring Boot.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.doc.ShowDoc> showDocServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.doc.ShowDoc> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.doc.ShowDoc(), "/showdoc.do");
            registration.setName("ShowDoc");
            return registration;
        }

        /**
         * Register Preview servlet for /preview.do paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.editor.PreviewServlet> previewServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.editor.PreviewServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.editor.PreviewServlet(), "/preview.do");
            registration.setName("previewServlet");
            return registration;
        }

        /**
         * Register FormMailAction servlet for /formmail.do paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.form.FormMailActionServlet> formMailServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.form.FormMailActionServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.form.FormMailActionServlet(), "/formmail.do");
            registration.setName("FormMailAction");
            return registration;
        }

        /**
         * Register OfflineAction servlet for /admin/offline.do paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.components.offline.OfflineAction> offlineServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.components.offline.OfflineAction> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.components.offline.OfflineAction(), "/admin/offline.do");
            registration.setName("offlineServlet");
            return registration;
        }

        /**
         * Register DeleteServlet for /admin/docdel.do paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.doc.DeleteServlet> deleteServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.doc.DeleteServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.doc.DeleteServlet(), "/admin/docdel.do");
            registration.setName("DelDoc");
            return registration;
        }

        /**
         * Register LogoffServlet for /logoff.do and /admin/logoff.do paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.LogoffServlet> logoffServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.LogoffServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.LogoffServlet(), "/logoff.do", "/admin/logoff.do");
            registration.setName("LogOff");
            return registration;
        }

        /**
         * Register MultipleFileUploadAction servlet for /admin/multiplefileupload.do paths.
         *
         * This legacy servlet parses the raw request body with Commons FileUpload.
         * It must not receive a Servlet multipart configuration, otherwise the
         * container may consume the body before the servlet's parser sees it.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.filebrowser.MultipleFileUploadAction> multipleFileUploadServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.filebrowser.MultipleFileUploadAction> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.filebrowser.MultipleFileUploadAction(), "/admin/multiplefileupload.do");
            registration.setName("MultipleFileUploadAction");
            return registration;
        }

        /**
         * Register ThumbServlet for /admin/thumb/*, /thumb/*, /tumbn/* paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.editor.ThumbServlet> thumbServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.editor.ThumbServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.editor.ThumbServlet(), "/admin/thumb/*", "/thumb/*", "/tumbn/*");
            registration.setName("thumbServlet");
            return registration;
        }

        /**
         * Register Captcha servlet for /captcha.jpg path.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.system.captcha.CaptchaServlet> captchaServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.system.captcha.CaptchaServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.system.captcha.CaptchaServlet(), "/captcha.jpg");
            registration.setName("captchaServlet");
            return registration;
        }

        /**
         * Register Elfinder servlet for /admin/elfinder-connector/ path.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.system.elfinder.ElfinderServlet> elfinderServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.system.elfinder.ElfinderServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.system.elfinder.ElfinderServlet(), "/admin/elfinder-connector/");
            registration.setName("elfinderServlet");
            return registration;
        }

        /**
         * Register Pdf servlet for /to.pdf/*, /topdf/* paths.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.components.pdf.PdfServlet> pdfServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.components.pdf.PdfServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.components.pdf.PdfServlet(), "/to.pdf/*", "/topdf/*");
            registration.setName("pdfServlet");
            return registration;
        }

        /**
         * Register XhrFileUpload servlet for /XhrFileUpload path.
         */
        @Bean
        @ConditionalOnBooleanProperty(name = "spring.servlet.multipart.enabled", matchIfMissing = true)
        public ServletRegistrationBean<sk.iway.iwcm.components.upload.XhrFileUploadServlet> xhrFileUploadServletRegistration(
                MultipartConfigElement multipartConfigElement) {
            ServletRegistrationBean<sk.iway.iwcm.components.upload.XhrFileUploadServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.components.upload.XhrFileUploadServlet(), "/XhrFileUpload");
            registration.setName("XhrFileUpload");
            registration.setMultipartConfig(multipartConfigElement);
            return registration;
        }

        /**
         * Register AdminUpload servlet for /admin/upload/chunk path.
         */
        @Bean
        @ConditionalOnBooleanProperty(name = "spring.servlet.multipart.enabled", matchIfMissing = true)
        public ServletRegistrationBean<sk.iway.iwcm.admin.upload.AdminUploadServlet> adminUploadServletRegistration(
                MultipartConfigElement multipartConfigElement) {
            ServletRegistrationBean<sk.iway.iwcm.admin.upload.AdminUploadServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.admin.upload.AdminUploadServlet(), "/admin/upload/chunk");
            registration.setName("AdminUpload");
            registration.setMultipartConfig(multipartConfigElement);
            return registration;
        }

        /**
         * Register ExportSync servlet for /export.sync path.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.sync.export.ExportSyncServlet> exportSyncServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.sync.export.ExportSyncServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.sync.export.ExportSyncServlet(), "/export.sync");
            registration.setName("exportSyncServlet");
            return registration;
        }

        /**
         * Register GetProtectedFile servlet for /files/protected/* path.
         * Migrated from web.xml servlet-mapping.
         */
        @Bean
        public ServletRegistrationBean<sk.iway.iwcm.doc.GetProtectedFileServlet> getProtectedFileServletRegistration() {
            ServletRegistrationBean<sk.iway.iwcm.doc.GetProtectedFileServlet> registration = new ServletRegistrationBean<>(
                new sk.iway.iwcm.doc.GetProtectedFileServlet(), "/files/protected/*");
            registration.setName("GetProtectedFile");
            return registration;
        }

        /**
         * Register StripesDispatcher servlet for *.action URLs.
         * Migrated from web.xml servlet-mapping.
         */
        @Bean
        public ServletRegistrationBean<net.sourceforge.stripes.controller.DispatcherServlet> stripesDispatcherRegistration() {
            ServletRegistrationBean<net.sourceforge.stripes.controller.DispatcherServlet> registration = new ServletRegistrationBean<>(
                new net.sourceforge.stripes.controller.DispatcherServlet(), "*.action");
            registration.setName("StripesDispatcher");
            registration.setLoadOnStartup(1);
            return registration;
        }
    }
}
