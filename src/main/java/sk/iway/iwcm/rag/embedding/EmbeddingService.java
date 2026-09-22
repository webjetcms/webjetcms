package sk.iway.iwcm.rag.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiClient;
import com.webjetcms.ai.AiProviderConfig;
import com.webjetcms.ai.AiProviderException;
import com.webjetcms.ai.EmbeddingInputType;
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
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

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
     * Creates document embeddings using provider and model settings from the selected assistant.
     *
     * Provider configuration is resolved from the current request.
     *
     * @param texts texts to embed
     * @param assistant assistant that selects the embedding provider and model
     * @param request request used to resolve provider configuration
     * @return generated embeddings and token usage, or an empty result when no texts are supplied
     * @throws ProviderCallException if the assistant or provider configuration is invalid, or the provider response
     *         cannot be used
     */
    public EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        HttpServletRequest request
    ) throws ProviderCallException {
        return embedWithUsage(texts, assistant, request, EmbeddingInputType.DOCUMENT);
    }

    /**
     * Creates embeddings of the specified input type using request-scoped provider configuration.
     *
     * @param texts texts to embed
     * @param assistant assistant that selects the embedding provider and model
     * @param request request used to resolve provider configuration
     * @param inputType embedding input type
     * @return generated embeddings and token usage, or an empty result when no texts are supplied
     * @throws ProviderCallException if the assistant or provider configuration is invalid, or the provider response
     *         cannot be used
     */
    public EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        HttpServletRequest request,
        EmbeddingInputType inputType
    ) throws ProviderCallException {
        return embedWithUsage(texts, assistant, request, null, inputType);
    }

    /**
     * Creates document embeddings using provider configuration resolved for the specified domain.
     *
     * @param texts texts to embed
     * @param assistant assistant that selects the embedding provider and model
     * @param domainName domain whose provider configuration should be used
     * @return generated embeddings and token usage, or an empty result when no texts are supplied
     * @throws ProviderCallException if the assistant or provider configuration is invalid, or the provider response
     *         cannot be used
     */
    public EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        String domainName
    ) throws ProviderCallException {
        return embedWithUsage(texts, assistant, domainName, EmbeddingInputType.DOCUMENT);
    }

    /**
     * Creates embeddings of the specified input type using domain-scoped provider configuration.
     *
     * @param texts texts to embed
     * @param assistant assistant that selects the embedding provider and model
     * @param domainName domain whose provider configuration should be used
     * @param inputType embedding input type
     * @return generated embeddings and token usage, or an empty result when no texts are supplied
     * @throws ProviderCallException if the assistant or provider configuration is invalid, or the provider response
     *         cannot be used
     */
    public EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        String domainName,
        EmbeddingInputType inputType
    ) throws ProviderCallException {
        return embedWithUsage(texts, assistant, null, domainName, inputType);
    }

    /**
     * Creates and validates an embedding batch using configuration resolved from the supplied context.
     *
     * Request-scoped configuration takes precedence over domain-scoped configuration. When neither context is
     * supplied, the provider's default configuration is used. Returned vector counts and dimensions are validated
     * before the result is returned.
     *
     * @param texts texts to embed
     * @param assistant assistant that selects the embedding provider and model
     * @param servletRequest optional request used to resolve provider configuration
     * @param domainName optional domain used when no request is supplied
     * @param inputType embedding input type
     * @return generated embeddings and token usage, or an empty result when no texts are supplied
     * @throws ProviderCallException if configuration is invalid or the provider response is unusable
     */
    private EmbeddingBatchResult embedWithUsage(
        List<String> texts,
        AssistantDefinitionEntity assistant,
        HttpServletRequest servletRequest,
        String domainName,
        EmbeddingInputType inputType
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
            .options(new EmbeddingOptions(dimensions, inputType))
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
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            return Constants.getInt("ragEmbeddingDimensions");
        }
    }

    /**
     * Resolves provider configuration from the most specific available context.
     *
     * @param providerId provider identifier
     * @param request optional request context, which takes precedence over the domain
     * @param domainName optional domain used when no request is supplied
     * @return resolved provider configuration
     * @throws ProviderCallException if the provider is unavailable or configuration cannot be resolved
     */
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
