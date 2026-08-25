package sk.iway.iwcm.rag.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.rag.embedding.EmbeddingService;

/**
 * Records embedding token usage into AI statistics using dedicated system assistants.
 */
@Service
public class RagEmbeddingStatService {

    public static final String GROUP_INDEXING = "90-embedding-indexing";
    public static final String GROUP_SEARCH = "91-embedding-search";
    public static final String NAME_INDEXING = "RAG-EMB-INDEX";
    public static final String NAME_SEARCH = "RAG-EMB-SEARCH";

    private final AssistantDefinitionRepository assistantRepository;
    private final AiStatRepository aiStatRepository;

    @Autowired
    public RagEmbeddingStatService(AssistantDefinitionRepository assistantRepository,
                                   AiStatRepository aiStatRepository) {
        this.assistantRepository = assistantRepository;
        this.aiStatRepository = aiStatRepository;
    }

    public AssistantDefinitionEntity getIndexingAssistant() {
        return getOrCreateAssistant(NAME_INDEXING, GROUP_INDEXING, CloudToolsForCore.getDomainId());
    }

    public AssistantDefinitionEntity getSearchAssistant() {
        return getOrCreateAssistant(NAME_SEARCH, GROUP_SEARCH, CloudToolsForCore.getDomainId());
    }

    public void recordIndexingTokens(AssistantDefinitionEntity assistant, int usedTokens) {
        recordTokens(assistant, usedTokens);
    }

    public void recordSearchTokens(AssistantDefinitionEntity assistant, int usedTokens) {
        recordTokens(assistant, usedTokens);
    }

    private void recordTokens(AssistantDefinitionEntity assistant, int usedTokens) {
        if (usedTokens <= 0 || assistant == null || assistant.getId() == null) return;
        AiStatService.addRecord(assistant.getId(), usedTokens, aiStatRepository, null);
    }

    private synchronized AssistantDefinitionEntity getOrCreateAssistant(String name, String groupName, Integer domainId) {
        Optional<AssistantDefinitionEntity> existing = assistantRepository.findFirstByNameAndDomainIdOrderByIdAsc(name, domainId);
        if (existing.isPresent()) return existing.get();

        String providerId = getDefaultProviderId();
        try {
            AssistantDefinitionEntity created = buildAssistant(name, groupName, providerId, domainId);
            created = assistantRepository.save(created);
            AiAssistantsService.clearCache();
            return created;
        } catch (RuntimeException ex) {
            Logger.error(RagEmbeddingStatService.class, "Failed to create RAG embedding stats assistant for name=" + name + ", provider=" + providerId + ", domainId=" + domainId + ", error=" + ex.getMessage());
            Optional<AssistantDefinitionEntity> fallback = assistantRepository.findFirstByNameAndDomainIdOrderByIdAsc(name, domainId);
            return fallback.orElse(null);
        }
    }

    private String getDefaultProviderId() {
        String providerId = Constants.getString("ragEmbeddingProvider");
        return Tools.isEmpty(providerId) ? "openai" : providerId.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private AssistantDefinitionEntity buildAssistant(String name, String groupName, String providerId, Integer domainId) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setName(name);
        assistant.setDescription(GROUP_INDEXING.equals(groupName) ? "System assistant for embedding indexing statistics" : "System assistant for embedding search statistics");
        assistant.setAction("text_embedding");
        assistant.setClassName(EmbeddingService.class.getName());
        assistant.setFieldFrom("");
        assistant.setFieldTo("semanticSearchEmbedding");
        assistant.setProvider(providerId);
        assistant.setInstructions(GROUP_INDEXING.equals(groupName) ? "System assistant for embedding indexing token statistics." : "System assistant for embedding search token statistics.");
        assistant.setModel(Constants.getString("ragEmbeddingModel"));
        assistant.setGroupName(groupName);
        assistant.setUserPromptEnabled(false);
        assistant.setUserPromptLabel("");
        assistant.setIcon("database-search");
        assistant.setKeepHtml(false);
        assistant.setUseStreaming(false);
        assistant.setUseTemporal(false);
        assistant.setActive(false);
        assistant.setDomainId(domainId);
        return assistant;
    }
}
