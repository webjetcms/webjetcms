package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;

/** Verifies document queue validation and unique source entries. */
class IndexQueueServiceTest {
    private final IndexQueueRepository queue = mock(IndexQueueRepository.class);
    private final IndexQueueService service = new IndexQueueService(queue, mock(EmbeddingChunkRepository.class));

    /** Invalid source IDs are rejected before any existing queue entries are removed. */
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0})
    void rejectsInvalidSourceIds(Integer id) {
        assertThrows(IllegalArgumentException.class,
            () -> service.addToQueue(Arrays.asList(1, id), RagEntityType.DOCUMENT, RagIndexAction.INDEX, 1));
        verifyNoInteractions(queue);
    }

    /** Missing accounting domains produce validation errors rather than null-pointer failures. */
    @Test
    void rejectsNullAccountingDomain() {
        assertThrows(IllegalArgumentException.class, () -> service.getQueued(RagEntityType.DOCUMENT, RagIndexAction.INDEX, null));
        verifyNoInteractions(queue);
    }

    /** Repeated source IDs are queued once, while an empty selection performs no database operation. */
    @Test
    void queuesUniqueSourcesAndSkipsEmptySelections() {
        service.addToQueue(List.of(), RagEntityType.DOCUMENT, RagIndexAction.INDEX, 1);
        verifyNoInteractions(queue);
        service.addToQueue(List.of(2, 2, 3), RagEntityType.DOCUMENT, RagIndexAction.INDEX, 1);
        verify(queue).deleteByEntityTypeAndEntityId(RagEntityType.DOCUMENT, List.of(2L, 3L), 1);
        ArgumentCaptor<List<IndexQueueEntity>> saved = ArgumentCaptor.captor();
        verify(queue).saveAll(saved.capture());
        assertEquals(List.of(2L, 3L), saved.getValue().stream().map(IndexQueueEntity::getEntityId).toList());
    }
}
