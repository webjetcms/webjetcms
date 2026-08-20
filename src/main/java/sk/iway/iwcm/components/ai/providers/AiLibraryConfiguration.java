package sk.iway.iwcm.components.ai.providers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.provider.gemini.GeminiProvider;
import com.webjetcms.ai.provider.openai.OpenAiProvider;
import com.webjetcms.ai.provider.openrouter.OpenRouterProvider;

/** Wires library-owned provider clients into the WebJET application lifecycle. */
@Configuration
public class AiLibraryConfiguration {

    @Bean(destroyMethod = "close")
    public AiClient webjetAiClient() {
        return AiClient.of(
            new OpenAiProvider(),
            new GeminiProvider(),
            new OpenRouterProvider()
        );
    }
}
