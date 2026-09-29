package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.webjetcms.ai.EmbeddingInputType;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.indexing.MarkdownContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.vectorstore.VectorStore;

/**
 * Tests vector reuse, shared source metadata, accounting-domain usage, and preservation after provider failure.
 */
class MarkdownIndexServiceTest {
    private static final String ROOT = "/admin/docs/test";
    private final SlidingWindowChunker chunker = mock(SlidingWindowChunker.class);
    private final EmbeddingService embeddings = mock(EmbeddingService.class);
    private final RagEmbeddingStatService statistics = mock(RagEmbeddingStatService.class);
    private final EmbeddingChunkRepository repository = mock(EmbeddingChunkRepository.class);
    private final VectorStore store = mock(VectorStore.class);
    private final AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
    private final ArgumentCaptor<List<EmbeddingChunkEntity>> savedChunks = ArgumentCaptor.captor();
    private final MarkdownIndexService service = new MarkdownIndexService(new MarkdownSourceService(),
        new MarkdownContentExtractor(), chunker, embeddings, statistics, repository, store);

    @BeforeEach
    void setup() {
        assistant.setProvider("local");
        assistant.setDomainId(2);
        assistant.setModel("model");
        when(embeddings.getDimensions()).thenReturn(2);
        when(repository.saveAllAndFlush(anyList())).thenAnswer(call -> {
            List<EmbeddingChunkEntity> rows = call.getArgument(0);
            for (int i = 0; i < rows.size(); i++) rows.get(i).setId((long) i + 1);
            return rows;
        });
    }

    /**
     * Verifies that unchanged vectors are reused, source metadata uses domain zero, and new embedding usage is charged to the assistant owner.
     */
    @Test
    void reusesUnchangedVectorsAndStoresSharedMetadataWithOwningDomainUsage() throws Exception {
        float[] reused = {1, 0};
        float[] updated = {0, 1};
        long id = MarkdownIndexService.entityId(ROOT, "sk/guide.md", 0);
        when(chunker.chunk("# Guide")).thenReturn(List.of("unchanged", "updated"));
        when(store.getExistingEmbeddingsByHash("MARKDOWN", id, "local", "model", 0))
            .thenReturn(Map.of(MarkdownIndexService.hash("unchanged"), reused));
        when(embeddings.embedWithUsage(List.of("updated"), assistant, "billing.example", EmbeddingInputType.DOCUMENT))
            .thenReturn(new EmbeddingBatchResult(List.of(updated), 7));

        service.indexFile(ROOT, "sk/guide.md", "# Guide", assistant, List.of(), "billing.example");

        verify(embeddings).embedWithUsage(List.of("updated"), assistant, "billing.example", EmbeddingInputType.DOCUMENT);
        verify(statistics).recordIndexingTokens(assistant, 7, 2);
        verify(repository).saveAllAndFlush(savedChunks.capture());
        assertEquals(2, savedChunks.getValue().size());
        EmbeddingChunkEntity row = savedChunks.getValue().get(0);
        assertEquals(ROOT + "/sk/guide.md", row.getSourcePath());
        assertEquals("Guide", row.getSourceTitle());
        assertEquals("sk", row.getLanguage());
        assertEquals(0, row.getDomainId());
        verify(store).updateEmbeddingBatch(List.of(1L, 2L), List.of(reused, updated));
    }

    /**
     * Verifies that an embedding provider failure propagates before any stored chunks are replaced.
     */
    @Test
    void providerFailureKeepsPreviousIndex() throws Exception {
        when(chunker.chunk("changed")).thenReturn(List.of("changed"));
        when(embeddings.embedWithUsage(anyList(), eq(assistant), anyString(), any()))
            .thenThrow(new IllegalStateException("Provider unavailable"));
        assertThrows(IllegalStateException.class,
            () -> service.indexFile(ROOT, "sk/guide.md", "changed", assistant, List.of(), "billing.example"));
        verifyNoInteractions(repository);
    }

    /** Reindexes legacy raw chunks even when the source hash matches, using cleaned text for chunking and embedding. */
    @Test
    void cleansBeforeChunkingAndReindexesPreviouslyUncleanedContent() throws Exception {
        String markdown = "# Guide\n\nRead [settings](../settings.md).\n\n![](screen.png)\n<!-- internal -->";
        String cleaned = "# Guide\n\nRead settings.";
        String sourceHash = MarkdownIndexService.hash(markdown);
        long id = MarkdownIndexService.entityId(ROOT, "en/guide.md", 0);
        EmbeddingChunkEntity existing = new EmbeddingChunkEntity();
        existing.setChunkIndex(0);
        existing.setEmbeddingProvider("local");
        existing.setEmbeddingModel("model");
        existing.setSourceHash(sourceHash);
        existing.setContentHash(sourceHash);
        existing.setDimensions(2);
        existing.setLanguage("en");
        existing.setStatus(EmbeddingChunkStatus.COMPLETED);
        when(chunker.chunk(cleaned)).thenReturn(List.of(cleaned));
        when(store.getExistingEmbeddingsByHash("MARKDOWN", id, "local", "model", 0))
            .thenReturn(Map.of(sourceHash, new float[] {1, 0}));
        when(embeddings.embedWithUsage(List.of(cleaned), assistant, "billing.example", EmbeddingInputType.DOCUMENT))
            .thenReturn(new EmbeddingBatchResult(List.of(new float[] {0, 1}), 5));

        service.indexFile(ROOT, "en/guide.md", markdown, assistant, List.of(existing), "billing.example");

        verify(chunker).chunk(cleaned);
        verify(embeddings).embedWithUsage(List.of(cleaned), assistant, "billing.example", EmbeddingInputType.DOCUMENT);
        verify(repository).saveAllAndFlush(savedChunks.capture());
        assertEquals(1, savedChunks.getValue().size());
        EmbeddingChunkEntity row = savedChunks.getValue().get(0);
        assertEquals(cleaned, row.getChunkText());
        assertEquals(MarkdownIndexService.hash(cleaned), row.getContentHash());
        assertEquals(sourceHash, row.getSourceHash());
        assertEquals(ROOT + "/en/guide.md", row.getSourcePath());
        assertEquals("Guide", row.getSourceTitle());
    }

    /** Deletes stale chunks without calling the embedding provider when cleanup leaves no content. */
    @Test
    void removesChunksForNoiseOnlySources() throws Exception {
        when(chunker.chunk("")).thenReturn(List.of());
        long id = MarkdownIndexService.entityId(ROOT, "en/empty.md", 0);

        service.indexFile(ROOT, "en/empty.md", "![](screen.png)\n\n<!-- internal -->", assistant, List.of(), "billing.example");

        verify(repository).deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, id, 0);
        verifyNoInteractions(embeddings, statistics, store);
    }
}
