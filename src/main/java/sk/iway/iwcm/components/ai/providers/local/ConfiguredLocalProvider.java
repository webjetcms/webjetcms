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

    /**
     * Closes the lazily opened delegate at most once.
     *
     * @throws Exception if the delegate cannot be closed
     */
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

    /**
     * Returns the active delegate, opening the configured model bundle on first use.
     *
     * @return active local provider delegate
     * @throws AiProviderException if the provider is closed, the bundle is not configured, the path is invalid,
     *         or the bundle cannot be opened
     */
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

    /**
     * Reads the global bundle path without inheriting a tenant-specific request context.
     *
     * @return configured bundle path, possibly empty when no path is configured
     */
    private String configuredBundlePath() {
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            return Constants.getString(bundlePathConstant);
        }
    }

    /** Opens a provider delegate from a configured local model bundle. */
    protected interface ProviderOpener {

        /**
         * Opens a provider for the supplied model bundle.
         *
         * @param bundle path to the model bundle
         * @return opened provider delegate
         * @throws AiProviderException if the bundle cannot be opened or validated
         */
        AiProvider open(Path bundle) throws AiProviderException;
    }
}
