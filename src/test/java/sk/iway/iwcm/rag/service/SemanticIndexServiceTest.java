package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.webjetcms.ai.EmbeddingInputType;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.indexing.DocDetailsContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.jpa.IndexQueueEntity;
import sk.iway.iwcm.rag.jpa.IndexQueueRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.vectorstore.VectorStore;

/**
 * Tests queue snapshots, retries, source routing, and domain ownership during document indexing.
 */
class SemanticIndexServiceTest {

    /**
     * Verifies that Markdown IDs beyond the integer range reach the Markdown handler and only successful queue entries are removed.
     */
    @Test
    void processesMarkdownLongIdsAndRetainsOnlyFailedFilesForRetry() {
        TestContext context = new TestContext();
        IndexQueueEntity succeeded = queueItem(21L, 1, 2);
        succeeded.setEntityType(RagEntityType.MARKDOWN);
        succeeded.setEntityId(Long.MAX_VALUE - 1);
        IndexQueueEntity failed = queueItem(22L, 2, 3);
        failed.setEntityType(RagEntityType.MARKDOWN);
        failed.setEntityId(Long.MAX_VALUE);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, Long.MAX_VALUE))
            .thenReturn(List.of(succeeded, failed));
        doThrow(new IllegalStateException("Temporary provider failure")).when(context.markdownIndexService).processQueueItem(failed);
        try (MockedStatic<Cache> cacheStatic = mockStatic(Cache.class);
             MockedStatic<Logger> ignored = mockStatic(Logger.class)) {
            cacheStatic.when(Cache::getInstance).thenReturn(context.cache);
            context.service.processQueue();
        }
        verify(context.markdownIndexService).processQueueItem(succeeded);
        verify(context.markdownIndexService).processQueueItem(failed);
        verify(context.queueRepository).deleteAllByIdInBatch(List.of(21L));
        verify(context.embeddingChunkRepository, never()).deleteByEntityTypeAndEntityIdAndDomainId(eq(RagEntityType.DOCUMENT), anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    /**
     * Verifies that document deletion uses the queued domain instead of the ambient domain and removes the processed entry.
     */
    @Test
    void deletesDocumentEmbeddingsUsingQueuedDomain() {
        TestContext context = new TestContext();
        IndexQueueEntity item = queueItem(11L, 123, 2);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, Long.MAX_VALUE)).thenReturn(List.of(item));

        try (MockedStatic<Cache> cacheStatic = mockStatic(Cache.class);
             MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class)) {
            cacheStatic.when(Cache::getInstance).thenReturn(context.cache);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);

            context.service.processQueue();
        }

        verify(context.embeddingChunkRepository).deleteByEntityTypeAndEntityIdAndDomainId(
            RagEntityType.DOCUMENT,
            123L,
            2
        );
        verify(context.queueRepository).deleteAllByIdInBatch(List.of(11L));
    }

    /**
     * Verifies that an unmapped document domain leaves the queue item pending without preparing the vector store or saving chunks.
     */
    @Test
    void rejectsUnmappedDocumentDomainInMultidomainMode() {
        TestContext context = new TestContext();
        IndexQueueEntity item = queueItem(14L, 126, 1);
        item.setAction(RagIndexAction.INDEX);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, Long.MAX_VALUE)).thenReturn(List.of(item));

        DocDetails document = new DocDetails();
        document.setDocId(126);
        DocDB docDB = mock(DocDB.class);
        when(docDB.getDoc(126)).thenReturn(document);
        when(docDB.getDomain(126)).thenReturn("missing.example");

        try (MockedStatic<Cache> cacheStatic = mockStatic(Cache.class);
             MockedStatic<DocDB> docDbStatic = mockStatic(DocDB.class);
             MockedStatic<GroupsDB> groupsDbStatic = mockStatic(GroupsDB.class);
             MockedStatic<InitServlet> initServletStatic = mockStatic(InitServlet.class);
             MockedStatic<Logger> ignored = mockStatic(Logger.class)) {
            cacheStatic.when(Cache::getInstance).thenReturn(context.cache);
            docDbStatic.when(DocDB::getInstance).thenReturn(docDB);
            groupsDbStatic.when(() -> GroupsDB.getDomainId("missing.example")).thenReturn(-1);
            initServletStatic.when(InitServlet::isTypeCloud).thenReturn(true);

            context.service.processQueue();
        }

        verify(context.queueRepository, never()).deleteAllByIdInBatch(anyList());
        verify(context.vectorStore, never()).isAvailable();
        verify(context.embeddingChunkRepository, never()).saveAllAndFlush(anyList());
    }

    /**
     * Verifies that a full batch of invalid entries does not prevent a later valid entry in the initial snapshot from being processed.
     */
    @Test
    void failedFullBatchDoesNotStarveLaterItems() {
        TestContext context = new TestContext();
        List<IndexQueueEntity> invalidItems = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            invalidItems.add(queueItem((long) i + 1, i + 1, 0));
        }
        IndexQueueEntity laterValidItem = queueItem(501L, 501, 2);
        when(context.queueRepository.findMaxId()).thenReturn(501L);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, 501L)).thenReturn(invalidItems);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(500L, 501L)).thenReturn(List.of(laterValidItem));

        try (MockedStatic<Cache> cacheStatic = mockStatic(Cache.class);
             MockedStatic<Logger> ignored = mockStatic(Logger.class)) {
            cacheStatic.when(Cache::getInstance).thenReturn(context.cache);

            context.service.processQueue();
        }

        verify(context.queueRepository).findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, 501L);
        verify(context.queueRepository).findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(500L, 501L);
        verify(context.queueRepository).deleteAllByIdInBatch(List.of(501L));
        verify(context.queueRepository, never()).deleteById(anyLong());
        verify(context.embeddingChunkRepository).deleteByEntityTypeAndEntityIdAndDomainId(
            RagEntityType.DOCUMENT,
            501L,
            2
        );
    }

    /**
     * Verifies that a failed deletion stays queued and is removed after succeeding on the next run.
     */
    @Test
    void keepsFailedItemAndProcessesItOnNextRun() {
        TestContext context = new TestContext();
        IndexQueueEntity item = queueItem(12L, 124, 2);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, Long.MAX_VALUE)).thenReturn(List.of(item));
        doThrow(new IllegalStateException("Database temporarily unavailable"))
            .doNothing()
            .when(context.embeddingChunkRepository)
            .deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.DOCUMENT, 124L, 2);

        try (MockedStatic<Cache> cacheStatic = mockStatic(Cache.class)) {
            cacheStatic.when(Cache::getInstance).thenReturn(context.cache);

            context.service.processQueue();
            verify(context.queueRepository, never()).deleteAllByIdInBatch(anyList());

            context.service.processQueue();
        }

        verify(context.embeddingChunkRepository, times(2)).deleteByEntityTypeAndEntityIdAndDomainId(
            RagEntityType.DOCUMENT,
            124L,
            2
        );
        verify(context.queueRepository).deleteAllByIdInBatch(List.of(12L));
    }

    /**
     * Verifies that document indexing uses its owning domain and assistant, initializes shared schema globally, and restores the caller context.
     */
    @Test
    void indexesDocumentUsingQueuedDomainAndRestoresRequestContext() throws Exception {
        String domainName = "tenant-two.example";
        TestContext context = new TestContext();
        IndexQueueEntity item = queueItem(13L, 125, 2);
        item.setAction(RagIndexAction.INDEX);
        when(context.queueRepository.findTop500ByIdGreaterThanAndIdLessThanEqualOrderByIdAsc(0L, Long.MAX_VALUE)).thenReturn(List.of(item));

        DocDetails document = new DocDetails();
        document.setDocId(125);
        document.setGroupId(7);
        GroupDetails group = new GroupDetails();
        group.setGroupId(7);
        group.setDomainName(domainName);
        group.setLng("sk");

        DocDB docDB = mock(DocDB.class);
        when(docDB.getDoc(125)).thenReturn(document);
        when(docDB.getDomain(125)).thenReturn(domainName);
        GroupsDB groupsDB = mock(GroupsDB.class);
        when(groupsDB.getGroup(7)).thenReturn(group);

        AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
        assistant.setProvider("provider-2");
        assistant.setModel("model-2");
        when(context.ragEmbeddingStatService.getIndexingAssistant(2)).thenReturn(assistant);
        when(context.embeddingService.getDimensions()).thenReturn(3);
        when(context.vectorStore.getExistingEmbeddingsByHash(
            DocDetailsContentExtractor.ENTITY_TYPE.name(),
            125L,
            "provider-2",
            "model-2",
            2
        )).thenReturn(Map.of());
        when(context.embeddingService.embedWithUsage(
            List.of("Indexed text"),
            assistant,
            domainName,
            EmbeddingInputType.DOCUMENT
        ))
            .thenReturn(new EmbeddingBatchResult(List.of(new float[] {0.1f, 0.2f, 0.3f}), 7));
        when(context.embeddingChunkRepository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<EmbeddingChunkEntity> chunks = invocation.getArgument(0);
            chunks.get(0).setId(91L);
            return chunks;
        });

        RequestBean previousRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
        RequestBean ambientRequestBean = new RequestBean();
        ambientRequestBean.setDomain("ambient.example");
        SetCharacterEncodingFilter.setCurrentRequestBean(ambientRequestBean);
        when(context.vectorStore.isAvailable()).thenAnswer(invocation -> {
            assertEquals(domainName, SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
            return true;
        });
        when(context.vectorStore.isAvailableAndInitialized()).thenAnswer(invocation -> {
            assertEquals(domainName, SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
            return false;
        });
        when(context.vectorStore.initializeSchema()).thenAnswer(invocation -> {
            assertNull(SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
            return true;
        });

        try (MockedStatic<Cache> cacheStatic = mockStatic(Cache.class);
             MockedStatic<CloudToolsForCore> cloudTools = mockStatic(CloudToolsForCore.class);
             MockedStatic<DocDB> docDbStatic = mockStatic(DocDB.class);
             MockedStatic<GroupsDB> groupsDbStatic = mockStatic(GroupsDB.class)) {
            cacheStatic.when(Cache::getInstance).thenReturn(context.cache);
            cloudTools.when(CloudToolsForCore::getDomainId).thenReturn(1);
            docDbStatic.when(DocDB::getInstance).thenReturn(docDB);
            docDbStatic.when(() -> DocDB.getRootGroupL(7, null, -1)).thenReturn(new int[] {7, 0, 0});
            groupsDbStatic.when(GroupsDB::getInstance).thenReturn(groupsDB);
            groupsDbStatic.when(() -> GroupsDB.getDomainId(domainName)).thenReturn(2);
            when(context.contentExtractor.extractText(document)).thenAnswer(invocation -> {
                assertEquals(domainName, SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
                return "Indexed text";
            });
            when(context.chunker.chunk("Indexed text")).thenReturn(List.of("Indexed text"));

            context.service.processQueue();

            assertSame(ambientRequestBean, SetCharacterEncodingFilter.getCurrentRequestBean());
        } finally {
            SetCharacterEncodingFilter.setCurrentRequestBean(previousRequestBean);
        }

        verify(context.vectorStore).isAvailable();
        verify(context.vectorStore).isAvailableAndInitialized();
        verify(context.vectorStore).initializeSchema();
        verify(context.ragEmbeddingStatService).getIndexingAssistant(2);
        verify(context.vectorStore).getExistingEmbeddingsByHash(
            DocDetailsContentExtractor.ENTITY_TYPE.name(),
            125L,
            "provider-2",
            "model-2",
            2
        );
        verify(context.embeddingChunkRepository).deleteByEntityTypeAndEntityIdAndEmbeddingProviderAndEmbeddingModelAndDomainId(
            RagEntityType.DOCUMENT,
            125L,
            "provider-2",
            "model-2",
            2
        );
        verify(context.ragEmbeddingStatService).recordIndexingTokens(assistant, 7, 2);
        verify(context.embeddingService).embedWithUsage(
            List.of("Indexed text"),
            assistant,
            domainName,
            EmbeddingInputType.DOCUMENT
        );
        verify(context.queueRepository).deleteAllByIdInBatch(List.of(13L));

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<EmbeddingChunkEntity>> chunksCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(context.embeddingChunkRepository).saveAllAndFlush(chunksCaptor.capture());
        List<EmbeddingChunkEntity> storedChunks = chunksCaptor.getValue();
        assertEquals(1, storedChunks.size());
        assertEquals(2, storedChunks.get(0).getDomainId());
        assertEquals("provider-2", storedChunks.get(0).getEmbeddingProvider());
        assertEquals("model-2", storedChunks.get(0).getEmbeddingModel());
        assertEquals(EmbeddingChunkStatus.PENDING, storedChunks.get(0).getStatus());

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<List<float[]>> embeddingsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(context.vectorStore).updateEmbeddingBatch(eq(List.of(91L)), embeddingsCaptor.capture());
        assertArrayEquals(new float[] {0.1f, 0.2f, 0.3f}, embeddingsCaptor.getValue().get(0));
    }

    private static IndexQueueEntity queueItem(long id, int entityId, int domainId) {
        IndexQueueEntity item = new IndexQueueEntity();
        item.setId(id);
        item.setEntityType(RagEntityType.DOCUMENT);
        item.setEntityId(entityId);
        item.setAction(RagIndexAction.DELETE);
        item.setDomainId(domainId);
        item.setCreateDate(new Date());
        return item;
    }

    /**
     * Provides isolated mocked collaborators and default vector-store readiness for queue-processing tests.
     */
    private static class TestContext {
        private final DocDetailsContentExtractor contentExtractor = mock(DocDetailsContentExtractor.class);
        private final SlidingWindowChunker chunker = mock(SlidingWindowChunker.class);
        private final EmbeddingService embeddingService = mock(EmbeddingService.class);
        private final VectorStore vectorStore = mock(VectorStore.class);
        private final IndexQueueRepository queueRepository = mock(IndexQueueRepository.class);
        private final RagEmbeddingStatService ragEmbeddingStatService = mock(RagEmbeddingStatService.class);
        private final EmbeddingChunkRepository embeddingChunkRepository = mock(EmbeddingChunkRepository.class);
        private final MarkdownIndexService markdownIndexService = mock(MarkdownIndexService.class);
        private final Cache cache = mock(Cache.class);
        private final SemanticIndexService service;

        private TestContext() {
            when(vectorStore.isAvailable()).thenReturn(true);
            when(vectorStore.isAvailableAndInitialized()).thenReturn(true);
            when(queueRepository.findMaxId()).thenReturn(Long.MAX_VALUE);
            service = new SemanticIndexService(
                contentExtractor,
                chunker,
                embeddingService,
                vectorStore,
                queueRepository,
                ragEmbeddingStatService,
                embeddingChunkRepository,
                markdownIndexService
            );
        }
    }
}
