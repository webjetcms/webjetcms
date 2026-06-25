package sk.iway.iwcm.rag.search;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PageParams;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.jpa.SupportedActions;
import sk.iway.iwcm.components.ai.rest.AiAssistantsService;
import sk.iway.iwcm.components.ai.rest.AiService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.rag.service.RagSettingsService;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;

@Service
public class RagService {

    private static final String RAG_ASSIST_NAME = "RAG-SEARCH";
    private static final String RAG_GROUP_NAME = "92-rag-answer";

    private static final String RAG_DEFAULT_ASSIST_PROVIDER = "openai";
    private static final String RAG_DEFAULT_ASSIST_MODEL = "gpt-5.4-mini";
    private static final String CANNOT_ANSWER_SENTINEL = "CANNOT_ANSWER_QUESTION";

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
        Prop prop = Prop.getInstance(request);
        if(Tools.isEmpty(question) || vectorChunkResults == null || vectorChunkResults.isEmpty()) {
            Logger.debug(RagService.class, "Cannot answer RAG question because question is empty or no retrieved chunks. question=" + question + ", vectorChunkResults=" + (vectorChunkResults == null ? "null" : vectorChunkResults.size()));
            return null;
        }

        PageParams pageParams = new PageParams(request);

        AssistantDefinitionEntity assistant = null;
        Long assistantId = Tools.getLongValue(pageParams.getValue("ragAssistantId", ""), -1L);
        if(assistantId != null && assistantId > 0) {
            Optional<AssistantDefinitionEntity> opt = assistantRepository.findByIdAndDomainId(assistantId, domainId);
            if(opt.isPresent()) {
                assistant = opt.get();
            }
        }

        if(assistant == null) {
            assistant = getOrCreateAssistant(domainId);
        }

        if(assistant == null) return null;

        // Post-process chunks: filter, merge, deduplicate, limit
        RagChunkPostProcessor postProcessor = new RagChunkPostProcessor(
                RagSettingsService.getRagAnswerTopK(pageParams),
                RagSettingsService.getRagAnswerMinSimilarity(pageParams),
                RagSettingsService.getRagAnswerMaxChunkGap(pageParams),
                RagSettingsService.getRagAnswerMaxBlocks(pageParams),
                RagSettingsService.getRagAnswerMaxCharacters(pageParams),
                RagSettingsService.getRagAnswerMaxMergedBlockCharacters(pageParams));

        List<MergedContextBlock> contextBlocks = postProcessor.process(vectorChunkResults);
        if(contextBlocks.isEmpty()) {
            Logger.debug(RagService.class, "Cannot answer RAG question because no chunks survived post-processing. question=" + question);
            return null;
        }

        InputDataDTO inputData = new InputDataDTO();
        inputData.setAssistantId(assistant.getId());
        inputData.setUserPrompt(question);
        inputData.setBonusParams(
            Map.of(
                "userQuestion", JSONObject.quote(question),
                "retrievedContext", buildChunksJson(contextBlocks).toString()
            )
        );
        inputData.setInputValueType(InputDataDTO.InputValueType.TEXT);
        inputData.setOutputValueType(InputDataDTO.InputValueType.TEXT);
        inputData.setTimestamp( Tools.getNow() );

        try {
            AssistantResponseDTO responseDto = aiService.getAiResponse(inputData, statRepo, assistantRepository, request);
            String ragAnswer = responseDto.getResponse();

            if(Tools.isEmpty(ragAnswer)) return null;

            ragAnswer = ragAnswer.trim();

            if(isCannotAnswerResponse(ragAnswer)) return null;

            return ragAnswer;
        } catch (Exception e) {
            Logger.error(RagService.class, "Error getting AI response for RAG question. assistantId=" + assistant.getId() + ", error=" + e.getMessage());
        }

        return null;
    }

    /**
     * Builds the structured JSON payload passed to the RAG answer assistant.
     * The payload keeps chunk text together with retrieval metadata so the prompt
     * contract remains explicit and future source fields can be added safely.
     *
     * @param contextBlocks merged context blocks selected for the answer prompt
     * @return JSON array with one object per context block
     */
    private JSONArray buildChunksJson(List<MergedContextBlock> contextBlocks) {
        if (contextBlocks == null || contextBlocks.isEmpty()) {
            return new JSONArray();
        }

        JSONArray chunksArray = new JSONArray();
        for (MergedContextBlock block : contextBlocks) {
            JSONObject chunk = new JSONObject();
            chunk.put("entityType", block.getEntityType());
            chunk.put("entityId", block.getEntityId());
            chunk.put("startChunkIndex", block.getStartChunkIndex());
            chunk.put("endChunkIndex", block.getEndChunkIndex());
            chunk.put("maxSimilarity", block.getMaxSimilarity());
            chunk.put("averageSimilarity", block.getAverageSimilarity());
            chunk.put("sourceChunkCount", block.getSourceChunkCount());
            chunk.put("text", block.getText());

            if(Tools.isNotEmpty(block.getSourceTitle()) || Tools.isNotEmpty(block.getSourceUrl())) {
                JSONObject source = new JSONObject();
                if(Tools.isNotEmpty(block.getSourceTitle())) source.put("title", block.getSourceTitle());
                if(Tools.isNotEmpty(block.getSourceUrl())) source.put("url", block.getSourceUrl());
                chunk.put("source", source);
            }

            chunksArray.put(chunk);
        }
        return chunksArray;
    }

    /**
     * Checks whether the assistant returned the cannot-answer sentinel. The
     * comparison accepts the new sentinel and the legacy misspelled value, with
     * light normalization for quotes, backticks, and a trailing period.
     *
     * @param ragAnswer raw assistant response
     * @return true when the response means the answer is not present in context
     */
    private boolean isCannotAnswerResponse(String ragAnswer) {
        if(Tools.isEmpty(ragAnswer)) return false;

        String normalized = ragAnswer.trim();
        boolean changed;
        do {
            changed = false;
            if(normalized.endsWith(".")) {
                normalized = normalized.substring(0, normalized.length() - 1).trim();
                changed = true;
            }
            if(normalized.length() >= 2 && (
                    (normalized.startsWith("\"") && normalized.endsWith("\"")) ||
                    (normalized.startsWith("'") && normalized.endsWith("'")) ||
                    (normalized.startsWith("`") && normalized.endsWith("`")))) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
                changed = true;
            }
        } while(changed);

        return CANNOT_ANSWER_SENTINEL.equalsIgnoreCase(normalized);
    }

    private AssistantDefinitionEntity getOrCreateAssistant(Integer domainId) {
        Optional<AssistantDefinitionEntity> existing = assistantRepository.findFirstByClassNameAndDomainIdOrderByIdAsc(RagService.class.getName(), domainId);
        if (existing.isPresent()) return existing.get();

        try {
            AssistantDefinitionEntity created = buildAssistant(domainId);
            created = assistantRepository.save(created);
            AiAssistantsService.clearCache();
            return created;
        } catch (RuntimeException ex) {
            Logger.error(RagService.class, "Error creating RAG answer assistant for domainId=" + domainId + ", error=" + ex.getMessage());
            return null;
        }
    }

    private AssistantDefinitionEntity buildAssistant(Integer domainId) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setName(RAG_ASSIST_NAME);
        assistant.setDescription("RAG search assistant.");
        assistant.setAction( SupportedActions.GENERATE_TEXT.getAction() );
        assistant.setClassName( RagService.class.getName() );
        assistant.setFieldFrom("");
        assistant.setFieldTo("semanticSearchEmbedding");
        assistant.setProvider(RAG_DEFAULT_ASSIST_PROVIDER);
        assistant.setInstructions( getInstructions() );
        assistant.setModel( getDefaultAnswerModel() );
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

    /**
     * Resolves the default model for newly created RAG answer assistants.
     *
     * @return configured ragAnswerModel value or the built-in fallback model
     */
    private String getDefaultAnswerModel() {
        String model = Constants.getString("ragAnswerModel");
        if(Tools.isEmpty(model)) return RAG_DEFAULT_ASSIST_MODEL;
        return model;
    }

    private String getInstructions() {
       return """
            {
                "role": "You answer questions using only retrieved document context.",
                "task": "Answer userQuestion using retrievedContext.",
                "userQuestion": {userQuestion},
                "instructions": [
                    "Use only retrievedContext as the source of truth.",
                    "Do not use outside knowledge, guess, or add unsupported facts.",
                    "If the answer is missing, respond with exactly: \\"CANNOT_ANSWER_QUESTION\\"",
                    "If only partly answered, give the supported part and say what is missing.",
                    "Merge overlapping chunks, remove repeated wording, and keep exact facts such as names, numbers, dates, limits, conditions, and exceptions.",
                    "If chunks conflict, state the conflict and do not choose a side unless the context resolves it.",
                    "Do not mention embeddings, vectors, similarity scores, chunking, retrieval, RAG, or internal details.",
                    "Do not cite sources unless source.title or source.url is present in retrievedContext.",
                    "Answer in the same language as the userQuestion.",
                    "Format bullets with each item on a new line.",
                    "Do not add follow-up offers, suggestions, or questions at the end. Just answer the userQuestion."
                ],
                "retrievedContext": {retrievedContext}
            }
        """;
    }

}
