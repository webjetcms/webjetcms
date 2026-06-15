package sk.iway.iwcm.system.spring;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
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
}
