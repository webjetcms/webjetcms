package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorstore.VectorStore;

/**
 * Tests directory-scoped queue replacement, cross-domain pending work, and deletion without disk scans.
 */
class MarkdownQueueServiceTest {
    private static final String ROOT = "/admin/docs/test/sk";
    private final MarkdownSourceService sources = spy(new MarkdownSourceService());
    private final IndexQueueRepository queue = mock(IndexQueueRepository.class);
    private final EmbeddingChunkRepository chunks = mock(EmbeddingChunkRepository.class);
    private final VectorStore store = mock(VectorStore.class);
    private final MarkdownQueueService service = new MarkdownQueueService(sources, queue, chunks,
        mock(RagEmbeddingStatService.class), store);

    /**
     * Verifies that a nonrecursive selection replaces only matching pending work and records the requesting domain without reading stored chunks.
     */
    @Test
    void queuesSelectedFilesInRequestingDomainAndPreservesSiblingWork() throws Exception {
        doReturn(List.of(ROOT)).when(sources).getRoots();
        doReturn(List.of("users/README.md")).when(sources).getFiles(ROOT, "users", false);
        when(queue.findByEntityTypeAndSourcePathStartingWith(RagEntityType.MARKDOWN, ROOT + "/"))
            .thenReturn(List.of(queued(81, "users/README.md"), queued(82, "users/roles/README.md")));
        try (MockedStatic<CloudToolsForCore> domain = mockStatic(CloudToolsForCore.class)) {
            domain.when(CloudToolsForCore::getDomainId).thenReturn(2);
            assertEquals(1, service.enqueue(ROOT, RagIndexAction.INDEX, "users", false));
        }
        verify(queue).deleteAllByIdInBatch(List.of(81L));
        verify(queue).saveAll(argThat(entries -> {
            IndexQueueEntity item = entries.iterator().next();
            return (ROOT + "/users/README.md").equals(item.getSourcePath()) && item.getDomainId() == 2
                && item.getEntityId() == MarkdownIndexService.entityId(ROOT, "users/README.md", 0)
                && item.getAction() == RagIndexAction.INDEX;
        }));
        verifyNoInteractions(chunks, store);
    }

    /**
     * Verifies that deletion queues each selected stored or pending-only source once, preserves descendant work, and performs no disk scan.
     */
    @Test
    void deletesSelectedMissingAndPendingFilesOnceWithoutScanningDisk() throws Exception {
        doReturn(List.of(ROOT)).when(sources).getRoots();
        when(store.isAvailableAndInitialized()).thenReturn(true);
        when(chunks.findByEntityTypeAndDomainIdAndSourcePathStartingWith(RagEntityType.MARKDOWN, 0, ROOT + "/"))
            .thenReturn(List.of(chunk("users/deleted.md"), chunk("users/deleted.md"), chunk("users/roles/deleted.md")));
        when(queue.findByEntityTypeAndSourcePathStartingWith(RagEntityType.MARKDOWN, ROOT + "/"))
            .thenReturn(List.of(queued(84, "users/pending.md"), queued(85, "users/roles/pending.md")));
        try (MockedStatic<CloudToolsForCore> domain = mockStatic(CloudToolsForCore.class)) {
            domain.when(CloudToolsForCore::getDomainId).thenReturn(2);
            assertEquals(Map.of("totalDocuments", 2, "indexedDocuments", 1, "queuedDocuments", 0),
                service.getStats(ROOT, RagIndexAction.DELETE, "users", false));
            assertEquals(2, service.enqueue(ROOT, RagIndexAction.DELETE, "users", false));
        }
        verify(queue).deleteAllByIdInBatch(List.of(84L));
        verify(queue).saveAll(argThat(entries -> {
            List<IndexQueueEntity> saved = StreamSupport.stream(entries.spliterator(), false).toList();
            return saved.stream().map(IndexQueueEntity::getSourcePath).toList()
                .equals(List.of(ROOT + "/users/deleted.md", ROOT + "/users/pending.md"))
                && saved.stream().allMatch(item -> item.getAction() == RagIndexAction.DELETE && item.getDomainId() == 2);
        }));
        verify(sources, never()).getFiles(anyString(), anyString(), anyBoolean());
        verify(sources, never()).resolveRoot(anyString());
    }

    private IndexQueueEntity queued(long id, String path) {
        IndexQueueEntity item = new IndexQueueEntity();
        item.setId(id);
        item.setEntityType(RagEntityType.MARKDOWN);
        item.setEntityId(MarkdownIndexService.entityId(ROOT, path, 0));
        item.setSourcePath(ROOT + "/" + path);
        item.setAction(RagIndexAction.INDEX);
        item.setDomainId(3);
        return item;
    }

    private EmbeddingChunkEntity chunk(String path) {
        EmbeddingChunkEntity item = new EmbeddingChunkEntity();
        item.setEntityId(MarkdownIndexService.entityId(ROOT, path, 0));
        item.setSourcePath(ROOT + "/" + path);
        return item;
    }
}
