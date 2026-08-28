package sk.iway.iwcm.components.ai.providers.local.translate;

import org.springframework.stereotype.Component;

import com.webjetcms.ai.AiInputHandling;
import com.webjetcms.ai.AiOperation;
import com.webjetcms.ai.provider.local.LocalTranslationModelProvider;

import sk.iway.iwcm.components.ai.providers.local.ConfiguredLocalProvider;

/** Lazily opens the configured local Facebook M2M100 translation model. */
@Component
public final class LocalTranslateProvider extends ConfiguredLocalProvider {

    public static final String BUNDLE_PATH_CONSTANT = "ai_localTranslateModelBundlePath";
    public static final String MODEL_ID = "facebook/m2m100_418M";
    public static final int MAXIMUM_OUTPUT_TOKENS = 200;
    public static final String PROVIDER_ID = LocalTranslationModelProvider.PROVIDER_ID;

    public LocalTranslateProvider() {
        super(PROVIDER_ID, BUNDLE_PATH_CONSTANT, LocalTranslationModelProvider::open);
    }

    @Override
    public AiInputHandling inputHandling(AiOperation operation) {
        return operation == AiOperation.TEXT ? AiInputHandling.LITERAL : AiInputHandling.PROTECTED_PROMPT;
    }
}
