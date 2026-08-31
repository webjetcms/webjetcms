package sk.iway.iwcm.components.ai.providers.local.embedding;

import org.springframework.stereotype.Component;

import com.webjetcms.ai.provider.local.LocalEmbeddingModelProvider;

import sk.iway.iwcm.components.ai.providers.local.ConfiguredLocalProvider;

/** Lazily opens the globally configured local embedding model bundle. */
@Component
public final class LocalEmbeddingProvider extends ConfiguredLocalProvider {

    public static final String BUNDLE_PATH_CONSTANT = "ai_localEmbeddingModelBundlePath";
    public static final String MODEL_ID = "intfloat/multilingual-e5-base";
    public static final String PROVIDER_ID =
        LocalEmbeddingModelProvider.PROVIDER_ID + "_embedding_" + MODEL_ID;

    public LocalEmbeddingProvider() {
        super(PROVIDER_ID, BUNDLE_PATH_CONSTANT, LocalEmbeddingModelProvider::open);
    }
}
