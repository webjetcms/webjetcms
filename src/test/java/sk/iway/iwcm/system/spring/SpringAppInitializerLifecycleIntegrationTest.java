package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

class SpringAppInitializerLifecycleIntegrationTest {

    @Test
    void postInitializationCompletesBeforeCustomerRunners() {
        StartupSequence startupSequence = new StartupSequence(true);
        SpringApplication application = springApplication(startupSequence);

        try (ConfigurableApplicationContext applicationContext = application.run(
                "--spring.main.web-application-type=none")) {
            WebjetBootstrapState bootstrapState = applicationContext.getBean(WebjetBootstrapState.class);
            WebjetInitializationActions initializationActions = applicationContext.getBean(
                "testInitializationActions", WebjetInitializationActions.class
            );
            List<String> events = startupSequence.events();

            assertTrue(bootstrapState.isPostInitializationCompleted());
            assertEquals(4, events.size());
            assertEquals("post-initialization", events.get(0));
            assertEquals(
                Set.of("application-runner", "command-line-runner"),
                Set.copyOf(events.subList(1, 3))
            );
            assertEquals("ready", events.get(3));
            verify(initializationActions).initializeAfterSpring();
        }
    }

    @Test
    void failedPostInitializationPreventsCustomerRunners() {
        StartupSequence startupSequence = new StartupSequence(false);
        SpringApplication application = springApplication(startupSequence);

        assertThrows(IllegalStateException.class, () -> application.run(
            "--spring.main.web-application-type=none"
        ));

        assertEquals(List.of("post-initialization"), startupSequence.events());
    }

    private SpringApplication springApplication(StartupSequence startupSequence) {
        SpringApplication application = new SpringApplication(LifecycleTestConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setRegisterShutdownHook(false);
        application.addInitializers(applicationContext -> applicationContext.getBeanFactory()
            .registerSingleton("startupSequence", startupSequence));
        return application;
    }

    @Configuration(proxyBeanMethods = false)
    @Import(SpringAppInitializer.class)
    static class LifecycleTestConfiguration {

        @Bean(name = WebjetBootstrapState.BEAN_NAME)
        WebjetBootstrapState webjetBootstrapState() {
            return WebjetBootstrapState.initialized(WebjetBootstrapMode.PRODUCTION, true);
        }

        @Bean
        @Primary
        WebjetInitializationActions testInitializationActions(StartupSequence startupSequence) {
            WebjetInitializationActions initializationActions = mock(WebjetInitializationActions.class);
            when(initializationActions.initializeAfterSpring()).thenAnswer(invocation -> {
                startupSequence.add("post-initialization");
                return startupSequence.postInitializationSucceeds();
            });
            return initializationActions;
        }

        @Bean
        ApplicationRunner customerApplicationRunner(WebjetBootstrapState bootstrapState,
                StartupSequence startupSequence) {
            return arguments -> {
                assertTrue(bootstrapState.isPostInitializationCompleted(),
                    "WebJET post-initialization must complete before customer runners");
                startupSequence.add("application-runner");
            };
        }

        @Bean
        CommandLineRunner customerCommandLineRunner(WebjetBootstrapState bootstrapState,
                StartupSequence startupSequence) {
            return arguments -> {
                assertTrue(bootstrapState.isPostInitializationCompleted(),
                    "WebJET post-initialization must complete before customer runners");
                startupSequence.add("command-line-runner");
            };
        }

        @Bean
        ApplicationListener<ApplicationReadyEvent> applicationReadyProbe(StartupSequence startupSequence) {
            return event -> startupSequence.add("ready");
        }
    }

    static final class StartupSequence {

        private final List<String> events = new ArrayList<>();
        private final boolean postInitializationSucceeds;

        private StartupSequence(boolean postInitializationSucceeds) {
            this.postInitializationSucceeds = postInitializationSucceeds;
        }

        void add(String event) {
            events.add(event);
        }

        boolean postInitializationSucceeds() {
            return postInitializationSucceeds;
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }
}
