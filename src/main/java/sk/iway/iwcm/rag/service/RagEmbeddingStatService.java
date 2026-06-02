package sk.iway.iwcm.rag.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.components.ai.stat.rest.AiStatService;
import sk.iway.iwcm.rag.embedding.EmbeddingProvider;

/**
 * Records embedding token usage into AI statistics using dedicated system assistants.
 */
@Service
public class RagEmbeddingStatService {

    public static final String GROUP_INDEXING = "90-embedding-indexing";
    public static final String GROUP_SEARCH = "91-embedding-search";

    private static final String PROVIDER_OPENAI = "openai";

    private final AssistantDefinitionRepository assistantRepository;
    private final AiStatRepository aiStatRepository;

    @Autowired
    public RagEmbeddingStatService(AssistantDefinitionRepository assistantRepository, AiStatRepository aiStatRepository) {
        this.assistantRepository = assistantRepository;
        this.aiStatRepository = aiStatRepository;
    }

    public void recordIndexingTokens(int usedTokens) {
        recordTokens(usedTokens, GROUP_INDEXING);
    }

    public void recordSearchTokens(int usedTokens) {
        recordTokens(usedTokens, GROUP_SEARCH);
    }

    private void recordTokens(int usedTokens, String groupName) {
        if (usedTokens <= 0) return;

        Integer domainId = CloudToolsForCore.getDomainId();
        AssistantDefinitionEntity assistant = getOrCreateAssistant(groupName, domainId);
        if (assistant == null) {
            return;
        }

        AiStatService.addRecord(assistant.getId(), usedTokens, aiStatRepository, null);
    }

    private AssistantDefinitionEntity getOrCreateAssistant(String groupName, Integer domainId) {
        Optional<AssistantDefinitionEntity> existing = assistantRepository.findFirstByGroupNameAndProviderAndDomainId(groupName, PROVIDER_OPENAI, domainId);
        if (existing.isPresent()) return existing.get();

        try {
            AssistantDefinitionEntity created = buildAssistant(groupName, domainId);
            created = assistantRepository.save(created);
            AiAssistantsService.clearCache();
            return created;
        } catch (RuntimeException ex) {
            Logger.error(RagEmbeddingStatService.class, "Failed to create RAG embedding stats assistant for groupName=" + groupName + ", provider=" + PROVIDER_OPENAI + ", domainId=" + domainId + ", error=" + ex.getMessage());
            Optional<AssistantDefinitionEntity> fallback = assistantRepository.findFirstByGroupNameAndProviderAndDomainId(groupName, PROVIDER_OPENAI, domainId);
            return fallback.orElse(null);
        }
    }

    private AssistantDefinitionEntity buildAssistant(String groupName, Integer domainId) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setName(GROUP_INDEXING.equals(groupName) ? "RAG-EMB-INDEX" : "RAG-EMB-SEARCH");
        assistant.setDescription(GROUP_INDEXING.equals(groupName) ? "System assistant for embedding indexing statistics" : "System assistant for embedding search statistics");
        assistant.setAction("text_embedding");
        assistant.setClassName(EmbeddingProvider.class.getName());
        assistant.setFieldFrom("");
        assistant.setFieldTo("semanticSearchEmbedding");
        assistant.setProvider(PROVIDER_OPENAI);
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