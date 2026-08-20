package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class SpringBootStarterTest {

    @Test
    void commandLineArgumentsArePassedOnlyToApplicationRun() {
        SpringApplicationBuilder application = mock(SpringApplicationBuilder.class);
        String[] args = {"--server.port=0", "--spring.profiles.active=production"};

        SpringBootStarter.runApplication(application, args);

        verify(application).run(args);
        verify(application, never()).profiles(any(String[].class));
    }

    @Test
    void nullArgumentsAreNormalizedToAnEmptyArray() {
        SpringApplicationBuilder application = mock(SpringApplicationBuilder.class);

        SpringBootStarter.runApplication(application, null);

        verify(application).run(new String[0]);
    }

    @Test
    void embeddedTomcatAllowsTheExpectedNumberOfMultipartParts() throws Exception {
        assertEquals("1000", PropertiesLoaderUtils.loadProperties(
            new ClassPathResource("application.properties")
        ).getProperty("server.tomcat.max-part-count"));
    }
}
