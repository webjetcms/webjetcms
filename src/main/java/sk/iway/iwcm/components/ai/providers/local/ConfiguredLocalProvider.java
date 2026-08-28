package sk.iway.iwcm.components.ai.providers.local;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

import com.webjetcms.ai.AiProvider;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.AiRequest;
import com.webjetcms.ai.AiResponse;
import com.webjetcms.ai.AiStreamListener;
import com.webjetcms.ai.EmbeddingRequest;
import com.webjetcms.ai.EmbeddingResponse;
import com.webjetcms.ai.ModelInfo;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Shared lazy lifecycle for local model bundles configured through WebJET constants. */
public abstract class ConfiguredLocalProvider implements AiProvider {

    private final String providerId;
    private final String bundlePathConstant;
    private final ProviderOpener opener;
    private final Object lifecycleMonitor = new Object();
    private volatile AiProvider delegate;
    private volatile boolean closed;

    protected ConfiguredLocalProvider(String providerId, String bundlePathConstant, ProviderOpener opener) {
        this.providerId = providerId;
        this.bundlePathConstant = bundlePathConstant;
        this.opener = opener;
    }

    @Override
    public final String id() {
        return providerId;
    }

    @Override
    public final List<ModelInfo> listModels(AiProviderConfig config) throws AiProviderException {
        return provider().listModels(config);
    }

    @Override
    public final AiResponse execute(AiRequest request, AiProviderConfig config) throws AiProviderException {
        return provider().execute(request, config);
    }

    @Override
    public final EmbeddingResponse embed(EmbeddingRequest request, AiProviderConfig config) throws AiProviderException {
        return provider().embed(request, config);
    }

    @Override
    public final AiResponse stream(AiRequest request, AiProviderConfig config, AiStreamListener listener)
        throws AiProviderException {
        return provider().stream(request, config, listener);
    }

    public final boolean isConfigured() {
        return Tools.isNotEmpty(configuredBundlePath());
    }

    @Override
    public final void close() throws Exception {
        AiProvider providerToClose;
        synchronized (lifecycleMonitor) {
            if (closed) return;
            closed = true;
            providerToClose = delegate;
            delegate = null;
        }
        if (providerToClose != null) providerToClose.close();
    }

    private AiProvider provider() throws AiProviderException {
        AiProvider current = delegate;
        if (current != null) return current;

        synchronized (lifecycleMonitor) {
            if (closed) throw new AiProviderException(providerId, "Local provider is closed");
            current = delegate;
            if (current != null) return current;

            String bundlePath = configuredBundlePath();
            if (Tools.isEmpty(bundlePath)) {
                throw new AiProviderException(
                    providerId,
                    "Local model bundle is not configured; set " + bundlePathConstant
                );
            }
            try {
                current = opener.open(Path.of(bundlePath));
            } catch (InvalidPathException exception) {
                throw new AiProviderException(providerId, "Local model bundle path is invalid", exception);
            }
            delegate = current;
            return current;
        }
    }

    private String configuredBundlePath() {
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            return Constants.getString(bundlePathConstant);
        }
    }

    protected interface ProviderOpener {
        AiProvider open(Path bundle) throws AiProviderException;
    }
}
