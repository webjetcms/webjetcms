package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.components.ai.dto.AssistantResponseDTO;
import sk.iway.iwcm.components.ai.dto.InputDataDTO;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionRepository;
import sk.iway.iwcm.components.ai.rest.AiService;
import sk.iway.iwcm.components.ai.stat.jpa.AiStatRepository;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.TestRequest;

class RagServiceTest extends BaseWebjetTest {

    private AssistantDefinitionRepository assistantRepository;
    private AiService aiService;
    private AiStatRepository statRepository;
    private RagService ragService;
    private TestRequest request;

    /**
     * Creates isolated mocks and a request object for each RAG service test.
     */
    @BeforeEach
    void setUp() {
        assistantRepository = Mockito.mock(AssistantDefinitionRepository.class);
        aiService = Mockito.mock(AiService.class);
        statRepository = Mockito.mock(AiStatRepository.class);
        ragService = new RagService(assistantRepository, aiService, statRepository);
        request = new TestRequest();
    }

    /**
     * Verifies that answer generation sends structured context metadata and the quoted user question.
     */
    @Test
    void answerQuestionSendsStructuredContext() throws Exception {
        AssistantDefinitionEntity assistant = assistant(10L);
        when(assistantRepository.findFirstByClassNameAndDomainIdOrderByIdAsc(RagService.class.getName(), 1)).thenReturn(Optional.of(assistant));
        when(aiService.getAiResponse(any(), any(), any(), any())).thenReturn(response("Supported answer."));

        String answer = ragService.answerQuestion("What is supported?", 1, List.of(
            chunk(100L, 3, "The product supports invoices.", 0.91d),
            chunk(100L, 4, "supports invoices and payments.", 0.84d)
        ), request);

        assertEquals("Supported answer.", answer);

        ArgumentCaptor<InputDataDTO> captor = ArgumentCaptor.forClass(InputDataDTO.class);
        verify(aiService).getAiResponse(captor.capture(), any(), any(), any());

        InputDataDTO inputData = captor.getValue();
        assertEquals(10L, inputData.getAssistantId());
        assertEquals("\"What is supported?\"", inputData.getBonusParams().get("userQuestion"));

        JSONArray context = new JSONArray(inputData.getBonusParams().get("retrievedContext"));
        assertEquals(1, context.length());
        JSONObject first = context.getJSONObject(0);
        assertEquals("document", first.getString("entityType"));
        assertEquals(100L, first.getLong("entityId"));
        assertEquals(3, first.getInt("startChunkIndex"));
        assertEquals(4, first.getInt("endChunkIndex"));
        assertTrue(first.getString("text").contains("invoices"));
    }

    /**
     * Verifies that the current cannot-answer sentinel is mapped to the localized fallback text.
     */
    @Test
    void answerQuestionMapsCannotAnswerSentinelToLocalizedText() throws Exception {
        AssistantDefinitionEntity assistant = assistant(10L);
        when(assistantRepository.findFirstByClassNameAndDomainIdOrderByIdAsc(RagService.class.getName(), 1)).thenReturn(Optional.of(assistant));
        when(aiService.getAiResponse(any(), any(), any(), any())).thenReturn(response("`CANNOT_ANSWER_QUESTION`."));

        String answer = ragService.answerQuestion("Unknown?", 1, List.of(chunk(100L, 0, "Known text.", 0.91d)), request);

        assertEquals(Prop.getInstance(request).getText("components.search.rag_answer.cant_answer"), answer);
    }

    /**
     * Verifies that empty retrieval results return the fallback text without calling AI.
     */
    @Test
    void answerQuestionDoesNotCallAiWhenChunksAreEmpty() throws Exception {
        String answer = ragService.answerQuestion("Question?", 1, List.of(), request);

        assertEquals(Prop.getInstance(request).getText("components.search.rag_answer.cant_answer"), answer);
        verify(aiService, never()).getAiResponse(any(), any(), any(), any());
    }

    /**
     * Verifies that a missing assistant is created with the configured default answer model.
     */
    @Test
    void answerQuestionCreatesDefaultAssistantWithConfiguredAnswerModel() throws Exception {
        Constants.setString("ragAnswerModel", "gpt-5.4-mini");
        when(assistantRepository.findFirstByClassNameAndDomainIdOrderByIdAsc(RagService.class.getName(), 1)).thenReturn(Optional.empty());
        when(assistantRepository.save(any())).thenAnswer(invocation -> {
            AssistantDefinitionEntity created = invocation.getArgument(0);
            created.setId(20L);
            return created;
        });
        when(aiService.getAiResponse(any(), any(), any(), any())).thenReturn(response("Created answer."));

        String answer = ragService.answerQuestion("Question?", 1, List.of(chunk(100L, 0, "Known text.", 0.91d)), request);

        assertEquals("Created answer.", answer);

        ArgumentCaptor<AssistantDefinitionEntity> assistantCaptor = ArgumentCaptor.forClass(AssistantDefinitionEntity.class);
        verify(assistantRepository).save(assistantCaptor.capture());
        assertEquals("gpt-5.4-mini", assistantCaptor.getValue().getModel());
        assertTrue(assistantCaptor.getValue().getInstructions().contains("CANNOT_ANSWER_QUESTION"));
        assertTrue(assistantCaptor.getValue().getInstructions().contains("\"userQuestion\""));
    }

    /**
     * Creates a minimal assistant entity for mocked repository responses.
     *
     * @param id assistant ID to assign
     * @return assistant entity with the requested ID
     */
    private AssistantDefinitionEntity assistant(Long id) {
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setId(id);
        return assistant;
    }

    /**
     * Creates a minimal AI response DTO for mocked AI service responses.
     *
     * @param text response text to return from the assistant
     * @return AI response DTO with the supplied text
     */
    private AssistantResponseDTO response(String text) {
        AssistantResponseDTO response = new AssistantResponseDTO();
        response.setResponse(text);
        return response;
    }

    /**
     * Creates a vector search result with deterministic IDs for test fixtures.
     *
     * @param entityId source entity ID
     * @param chunkIndex chunk index inside the source entity
     * @param text chunk text
     * @param similarity vector similarity score
     * @return vector search result fixture
     */
    private VectorSearchResult chunk(Long entityId, int chunkIndex, String text, double similarity) {
        return new VectorSearchResult(Long.valueOf(entityId * 10 + chunkIndex), "document", entityId, chunkIndex, text, similarity);
    }
}
