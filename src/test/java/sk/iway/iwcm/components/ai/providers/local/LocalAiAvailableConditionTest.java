package sk.iway.iwcm.components.ai.providers.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProvider;

import sk.iway.iwcm.components.ai.providers.AiAssitantsInterface;
import sk.iway.iwcm.components.ai.providers.AiLibraryConfiguration;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;

/** Verifies that optional local AI components do not prevent the base AI client from starting. */
class LocalAiAvailableConditionTest {

    /** Starts the AI context with and without the local library visible to Spring. */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void startsWithOptionalLocalProviders(boolean localLibraryAvailable) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            if (!localLibraryAvailable) {
                context.setClassLoader(new ClassLoader(getClass().getClassLoader()) {
                    @Override
                    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                        if (name.startsWith("com.webjetcms.ai.provider.local.")
                            || name.startsWith("com.webjetcms.ai.local.")) {
                            throw new ClassNotFoundException(name);
                        }
                        return super.loadClass(name, resolve);
                    }
                });
            }
            context.register(AiLibraryConfiguration.class);
            context.registerBean(WebjetAiConfigurationService.class, () -> mock(WebjetAiConfigurationService.class));
            context.scan("sk.iway.iwcm.components.ai.providers.local");
            context.refresh();

            assertNotNull(context.getBean(AiClient.class));
            assertEquals(
                localLibraryAvailable
                    ? Set.of("localEmbeddingProvider", "localTextProvider", "localTranslateProvider") : Set.of(),
                context.getBeansOfType(AiProvider.class).keySet()
            );
            assertEquals(
                localLibraryAvailable
                    ? Set.of("localEmbeddingService", "localTextService", "localTranslateService") : Set.of(),
                context.getBeansOfType(AiAssitantsInterface.class).keySet()
            );
        }
    }
}
