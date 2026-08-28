package sk.iway.iwcm.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.EmbeddingOptions;
import com.webjetcms.ai.EmbeddingRequest;
import com.webjetcms.ai.EmbeddingResponse;
import com.webjetcms.ai.EmbeddingVector;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.providers.AiInterface;
import sk.iway.iwcm.components.ai.providers.ProviderCallException;
import sk.iway.iwcm.components.ai.providers.WebjetAiConfigurationService;

/** Adapts CMS assistant and domain context to the framework-neutral embedding action. */
@Service
public class EmbeddingService {

    private final AiClient aiClient;
    private final WebjetAiConfigurationService configurationService;
    private final List<AiInterface> providers;

    public EmbeddingService(
        AiClient aiClient,
        WebjetAiConfigurationService configurationService,
        List<AiInterface> providers
    ) {
        this.aiClient = aiClient;
        this.configurationService = configurationService;
        this.providers = providers;
    }

    /**
     * Creates embeddings using the provider and model stored in the selected system assistant.
     */
    public EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        HttpServletRequest request
    ) throws ProviderCallException {
        return embedWithUsage(texts, assistant, request, null);
    }

    /**
     * Creates embeddings using provider configuration resolved for the specified domain.
     */
    public EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        String domainName
    ) throws ProviderCallException {
        return embedWithUsage(texts, assistant, null, domainName);
    }

    private EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        HttpServletRequest servletRequest,
        String domainName
    ) throws ProviderCallException {
        if (texts == null || texts.isEmpty()) return EmbeddingBatchResult.empty();
        if (assistant == null) {
            throw new ProviderCallException("RAG embedding assistant is not available");
        }
        if (Tools.isEmpty(assistant.getProvider())) {
            throw new ProviderCallException("RAG embedding assistant has no provider configured");
        }
        String providerId = assistant.getProvider().trim().toLowerCase(Locale.ROOT);
        if (Tools.isEmpty(assistant.getModel())) {
            throw new ProviderCallException("RAG embedding assistant has no model configured");
        }

        int dimensions = getDimensions();
        if (dimensions < 1) {
            throw new ProviderCallException("RAG embedding dimensions must be greater than zero");
        }

        EmbeddingRequest request = EmbeddingRequest.builder()
            .model(assistant.getModel())
            .inputs(texts)
            .options(new EmbeddingOptions(dimensions))
            .build();

        try {
            EmbeddingResponse response = aiClient.embed(
                providerId,
                request,
                resolveConfiguration(providerId, servletRequest, domainName)
            );
            List<EmbeddingVector> vectors = response.embeddings();
            if (vectors.size() != texts.size()) {
                throw new ProviderCallException(
                    "Embedding count mismatch: expected " + texts.size() + ", got " + vectors.size()
                );
            }

            List<float[]> embeddings = new ArrayList<>(vectors.size());
            for (EmbeddingVector vector : vectors) {
                if (vector.dimensions() != dimensions) {
                    throw new ProviderCallException(
                        "Embedding dimension mismatch: expected " + dimensions + ", got " + vector.dimensions()
                    );
                }
                embeddings.add(vector.values());
            }
            return new EmbeddingBatchResult(
                embeddings,
                Tools.safeLongToInt(response.usage().totalTokens())
            );
        } catch (AiProviderException | IllegalArgumentException exception) {
            throw new ProviderCallException(exception.getMessage(), exception);
        }
    }

    /** Returns the dimension required by the configured pgvector column. */
    public int getDimensions() {
        return Constants.getInt("ragEmbeddingDimensions");
    }

    private AiProviderConfig resolveConfiguration(
        String providerId,
        HttpServletRequest request,
        String domainName
    ) throws ProviderCallException {
        AiInterface provider = findProvider(providerId);
        if (request != null) {
            return configurationService.resolve(provider, request);
        }
        if (Tools.isNotEmpty(domainName)) {
            return configurationService.resolveForDomain(provider, domainName);
        }
        return configurationService.resolve(provider);
    }

    private AiInterface findProvider(String providerId) throws ProviderCallException {
        for (AiInterface provider : providers) {
            if (provider.getProviderId().equals(providerId)) return provider;
        }
        throw new ProviderCallException("RAG embedding provider is not available: " + providerId);
    }
}
