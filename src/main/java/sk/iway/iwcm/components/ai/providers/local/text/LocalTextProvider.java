package sk.iway.iwcm.components.ai.providers.local.text;

import org.springframework.stereotype.Component;

import com.webjetcms.ai.AiInputHandling;
import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.provider.local.LocalGenerationModelProvider;

import sk.iway.iwcm.components.ai.providers.local.ConfiguredLocalProvider;

/** Lazily opens the configured local EuroLLM-1.7B-Instruct text model. */
@Component
public final class LocalTextProvider extends ConfiguredLocalProvider {

    public static final String BUNDLE_PATH_CONSTANT = "ai_localTextModelBundlePath";
    public static final String MODEL_ID = "utter-project/EuroLLM-1.7B-Instruct";
    public static final String PROVIDER_ID = LocalGenerationModelProvider.PROVIDER_ID;

    public LocalTextProvider() {
        super(PROVIDER_ID, BUNDLE_PATH_CONSTANT, LocalGenerationModelProvider::open);
    }

    @Override
    public AiInputHandling inputHandling(AiOperation operation) {
        return operation == AiOperation.TEXT ? AiInputHandling.LITERAL : AiInputHandling.PROTECTED_PROMPT;
    }

}
