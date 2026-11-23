package sk.iway.iwcm.components.ai.providers;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProvider;

/** Wires bundled and application-defined providers into the WebJET application lifecycle. */
@Configuration
public class AiLibraryConfiguration {

    @Bean(destroyMethod = "close")
    public AiClient webjetAiClient(ObjectProvider<AiProvider> customProviders) {
        return AiClient.discover(customProviders.stream().toArray(AiProvider[]::new));
    }
}
