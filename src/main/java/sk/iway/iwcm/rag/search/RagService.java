package sk.iway.iwcm.rag.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

@Service
public class RagService {

    private static final String RAG_ASSIST_NAME = "RAG-SEARCH";
    private static final String RAG_GROUP_NAME = "92-rag-search";

    private static final String RAG_DEFAULT_ASSIST_PROVIDER = "openai";
    private static final String RAG_DEFAULT_ASSIST_MODEL = "gpt-5.4-mini";

    private static final double DEFAULT_MIN_SIMILARITY = 0.3;
    private static final int DEFAULT_TOP_K = 10;
    private static final int DEFAULT_MAX_CHUNK_GAP = 1;
    private static final int DEFAULT_MAX_BLOCKS = 3;
    private static final int DEFAULT_MAX_CHARACTERS = 4500;
    private static final int DEFAULT_MAX_MERGED_BLOCK_CHARACTERS = 1800;

    private final AssistantDefinitionRepository assistantRepository;
    private final AiStatRepository statRepo;

    private final AiService aiService;

    @Autowired
    public RagService(AssistantDefinitionRepository assistantRepository, AiService aiService, AiStatRepository statRepo) {
        this.assistantRepository = assistantRepository;
        this.aiService = aiService;
        this.statRepo = statRepo;
    }

    public String answerQuestion(String question, Integer domainId, List<VectorSearchResult> vectorChunkResults, HttpServletRequest request) {
        AssistantDefinitionEntity assistant = getOrCreateAssistant(domainId);

        // Post-process chunks: filter, merge, deduplicate, limit
        RagChunkPostProcessor postProcessor = new RagChunkPostProcessor(
                DEFAULT_TOP_K, DEFAULT_MIN_SIMILARITY, DEFAULT_MAX_CHUNK_GAP, DEFAULT_MAX_BLOCKS, DEFAULT_MAX_CHARACTERS, DEFAULT_MAX_MERGED_BLOCK_CHARACTERS);
        List<MergedContextBlock> contextBlocks = postProcessor.process(vectorChunkResults);

        InputDataDTO inputData = new InputDataDTO();
        inputData.setAssistantId(assistant.getId());
        inputData.setUserPrompt(question);
        inputData.setBonusParams(
            Map.of(
                // "userQuestion", question,
                "chunks", buildChunksJson(contextBlocks).toString()
            )
        );
        inputData.setInputValueType(InputDataDTO.InputValueType.TEXT);
        inputData.setOutputValueType(InputDataDTO.InputValueType.TEXT);
        inputData.setTimestamp( Tools.getNow() );

        try {
            AssistantResponseDTO responseDto = aiService.getAiResponse(inputData, statRepo, assistantRepository, request);
            return responseDto.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private JSONArray buildChunksJson(List<MergedContextBlock> contextBlocks) {
        if (contextBlocks == null || contextBlocks.isEmpty()) {
            return new JSONArray();
        }

        JSONArray chunksArray = new JSONArray();
        for (MergedContextBlock block : contextBlocks) {
            chunksArray.put(block.getText());
        }
        return chunksArray;
    }

    private AssistantDefinitionEntity getOrCreateAssistant(Integer domainId) {
        Optional<AssistantDefinitionEntity> existing = assistantRepository.findFirstByClassNameAndDomainId(RagService.class.getName(), domainId);
        if (existing.isPresent()) return existing.get();

        try {
            AssistantDefinitionEntity created = buildAssistant(domainId);
            created = assistantRepository.save(created);
            AiAssistantsService.clearCache();
            return created;
        } catch (RuntimeException ex) {
            // Logger.error(RagEmbeddingStatService.class, "Failed to create RAG embedding stats assistant for groupName=" + groupName + ", provider=" + PROVIDER_OPENAI + ", domainId=" + domainId + ", error=" + ex.getMessage());
            // Optional<AssistantDefinitionEntity> fallback = assistantRepository.findFirstByGroupNameAndProviderAndDomainId(groupName, PROVIDER_OPENAI, domainId);
            // return fallback.orElse(null);
            return null;
        }
    }

    private AssistantDefinitionEntity buildAssistant(Integer domainId) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setName(RAG_ASSIST_NAME);
        assistant.setDescription("RAG search assistants.");
        assistant.setAction( SupportedActions.GENERATE_TEXT.getAction() );
        assistant.setClassName( RagService.class.getName() );
        assistant.setFieldFrom("");
        assistant.setFieldTo("semanticSearchEmbedding");
        assistant.setProvider(RAG_DEFAULT_ASSIST_PROVIDER);
        assistant.setInstructions( getInstructions() );
        assistant.setModel(RAG_DEFAULT_ASSIST_MODEL);
        assistant.setGroupName(RAG_GROUP_NAME);
        assistant.setUserPromptEnabled(false);
        assistant.setUserPromptLabel("");
        assistant.setIcon("database-search");
        assistant.setKeepHtml(false);
        assistant.setUseStreaming(false);
        assistant.setUseTemporal(false);
        assistant.setActive(true);
        assistant.setDomainId(domainId);
        return assistant;
    }

    private String getInstructions() {
       return """
            {
                "role": "You answer questions using only retrieved document context.",
                "task": "Answer the userQuestion using retrievedContext.",
                "instructions": [
                    "Use only retrievedContext as the source of truth.",
                    "Do not use outside knowledge, guess, or add unsupported facts.",
                    "If the answer is missing, say: \\"I don't have enough information in the provided context to answer that.\\"",
                    "If only partly answered, give the supported part and say what is missing.",
                    "Merge overlapping chunks, remove repeated wording, and keep exact facts such as names, numbers, dates, limits, conditions, and exceptions.",
                    "If chunks conflict, state the conflict and do not choose a side unless the context resolves it.",
                    "Do not mention embeddings, vectors, similarity scores, chunking, retrieval, RAG, or internal details.",
                    "Use citations only when source metadata is provided.",
                    "Answer in the same language as the userQuestion.",
                    "Format bullets with each item on a new line.",
                    "Do not add follow-up offers, suggestions, or questions at the end. Just answer the userQuestion."
                ],
                "retrievedContext": {chunks}
            }
        """;
    }

}
