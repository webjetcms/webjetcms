package sk.iway.iwcm.system.spring;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jpa.autoconfigure.JpaBaseConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.system.spring.components.SpringContext;

/**
 * Spring Boot 4.x application starter (hybrid approach).
 * This class provides the Spring Boot entry point while preserving
 * the existing WebApplicationInitializer-based initialization flow.
 *
 * For embedded server: ./gradlew bootRun
 * For external Tomcat 11 deployment: ./gradlew war
 */
@SpringBootApplication(
    exclude = {
        DataSourceAutoConfiguration.class,
        JpaBaseConfiguration.class,
        SecurityAutoConfiguration.class
    }
)
@ComponentScan({
    "sk.iway.iwcm",
    "sk.iway.basecms",
    "sk.iway.aceintegration",
    "sk.iway.iway"
})
@EnableAutoConfiguration
public class SpringBootStarter extends SpringBootServletInitializer {

    private static final String[] DEFAULT_PROFILES = {"default"};

    public static void main(String[] args) {
        Logger.info(SpringBootStarter.class, "=== WebJET CMS starting with Spring Boot 4.x ===");

        // Set default profiles
        if (args == null || args.length == 0) {
            args = DEFAULT_PROFILES;
        }

        // Run Spring Boot application context
        var context = new SpringApplicationBuilder(SpringBootStarter.class)
            .profiles(args)
            .properties(
                "spring.profiles.active:" + (System.getProperty("spring.profiles.active") != null ? System.getProperty("spring.profiles.active") : "default"),
                "server.servlet.context-path:/",
                "server.tomcat.basedir:."
            )
            .run(args);

        // Store Spring context reference for legacy code access
        SpringContext springContext = context.getBean(SpringContext.class);
        springContext.setApplicationContext(context);

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
}
