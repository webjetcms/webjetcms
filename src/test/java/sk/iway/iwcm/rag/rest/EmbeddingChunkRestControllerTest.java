package sk.iway.iwcm.rag.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.service.IndexQueueService;
import sk.iway.iwcm.rag.service.RagEmbeddingStatService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.PgVectorStore;

class EmbeddingChunkRestControllerTest {

    @Test
    void currentConfigurationUsesPersistedIndexingAssistant() {
        EmbeddingChunkRepository repository = mock(EmbeddingChunkRepository.class);
        RagEmbeddingStatService statService = mock(RagEmbeddingStatService.class);
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setProvider("GEMINI");
        assistant.setModel("gemini-embedding-001");
        when(statService.getIndexingAssistant()).thenReturn(assistant);
        EmbeddingChunkRestController controller = controller(repository, statService);

        Map<String, String> result = controller.getCurrentEmbeddingConfiguration();

        assertEquals("gemini", result.get("provider"));
        assertEquals("gemini-embedding-001", result.get("model"));
    }

    @Test
    void indexPreviewUsesCurrentAssistantProviderAndModel() {
        EmbeddingChunkRepository repository = mock(EmbeddingChunkRepository.class);
        RagEmbeddingStatService statService = mock(RagEmbeddingStatService.class);
        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setProvider("GEMINI");
        assistant.setModel("gemini-embedding-001");
        when(statService.getIndexingAssistant()).thenReturn(assistant);
        when(repository.findDistinctEntityIdsByEntityTypeAndEmbeddingProviderAndEmbeddingModelAndDomainId(
            RagEntityType.DOCUMENT, "gemini", "gemini-embedding-001", 1
        )).thenReturn(List.of(20, 30));
        EmbeddingChunkRestController controller = controller(repository, statService);

        Set<Integer> result = controller.getIndexedDocumentIds(RagIndexAction.INDEX, 1);

        assertEquals(Set.of(20, 30), result);
        verify(repository, never()).findDistinctEntityIdsByEntityTypeAndDomainId(RagEntityType.DOCUMENT, 1);
    }

    @Test
    void deletePreviewIncludesIndexesFromAllAssistantSettings() {
        EmbeddingChunkRepository repository = mock(EmbeddingChunkRepository.class);
        RagEmbeddingStatService statService = mock(RagEmbeddingStatService.class);
        when(repository.findDistinctEntityIdsByEntityTypeAndDomainId(RagEntityType.DOCUMENT, 1))
            .thenReturn(List.of(10, 20));
        EmbeddingChunkRestController controller = controller(repository, statService);

        Set<Integer> result = controller.getIndexedDocumentIds(RagIndexAction.DELETE, 1);

        assertEquals(Set.of(10, 20), result);
        verify(statService, never()).getIndexingAssistant();
    }

    private EmbeddingChunkRestController controller(EmbeddingChunkRepository repository, RagEmbeddingStatService statService) {
        return new EmbeddingChunkRestController(
            repository,
            mock(IndexQueueService.class),
            mock(PgVectorStore.class),
            statService
        );
    }
}
