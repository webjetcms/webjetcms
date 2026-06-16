package sk.iway.iwcm.system.spring;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import net.sourceforge.stripes.controller.StripesFilterIway;
import sk.iway.iwcm.system.context.ContextFilter;

/**
 * Spring Boot 4.x application starter.
 * This class provides the Spring Boot entry point for both embedded server
 * and WAR deployment to external Tomcat 11.
 *
 * For embedded server: ./gradlew bootRun
 * For external Tomcat 11 deployment: ./gradlew war
 */
@SpringBootApplication
@ComponentScan({
    "sk.iway.iwcm",
    "sk.iway.basecms",
    "sk.iway.aceintegration",
    "sk.iway.iway",
    "sk.iway.webjet.v9"
})
@EnableAutoConfiguration(exclude = {
    // JPA repositories auto-configuration is managed by BaseSpringConfig
    DataJpaRepositoriesAutoConfiguration.class,
    // Security auto-configuration is managed by SpringSecurityConf
    SecurityAutoConfiguration.class
})
public class SpringBootStarter extends SpringBootServletInitializer {

    private static final String[] DEFAULT_PROFILES = {"default"};

    public static void main(String[] args) {
        Logger.info(SpringBootStarter.class, "=== WebJET CMS starting with Spring Boot 4.x ===");

        // Set default profiles
        if (args == null || args.length == 0) {
            args = DEFAULT_PROFILES;
        }

        // Run Spring Boot application context
        new SpringApplicationBuilder(SpringBootStarter.class)
            .profiles(args)
            .properties(
                "spring.profiles.active:" + (System.getProperty("spring.profiles.active") != null ? System.getProperty("spring.profiles.active") : "default"),
                "spring.main.allow-bean-definition-overriding:true",
                "server.servlet.context-path:/",
                "server.tomcat.basedir:."
            )
            .run(args);

        Logger.info(SpringBootStarter.class, "Spring Boot context started successfully");
        Logger.info(SpringBootStarter.class, "=== WebJET CMS started ===");
    }

    /**
     * Support for deploying as WAR to external Tomcat.
     * Extends SpringBootServletInitializer to allow
     * ./gradlew war to produce a deployable WAR.
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(SpringBootStarter.class);
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
        registration.setOrder(2);
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
        registration.setOrder(3);
        registration.setName("Virtual Path Filter");
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
     */
    @Bean
    public ServletRegistrationBean<sk.iway.iwcm.filebrowser.MultipleFileUploadAction> multipleFileUploadServletRegistration() {
        ServletRegistrationBean<sk.iway.iwcm.filebrowser.MultipleFileUploadAction> registration = new ServletRegistrationBean<>(
            new sk.iway.iwcm.filebrowser.MultipleFileUploadAction(), "/admin/multiplefileupload.do");
        registration.setName("MultipleFileUploadAction");
        return registration;
    }
}
