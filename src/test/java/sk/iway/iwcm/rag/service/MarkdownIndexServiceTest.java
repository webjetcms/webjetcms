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
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker.Chunk;
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

    /** Reuses matching context, refreshes changed headings, and keeps source metadata and usage accounting intact. */
    @Test
    void reusesMatchingContextAndRefreshesChangedHeading() throws Exception {
        float[] reused = {1, 0};
        float[] updated = {0, 1};
        String markdown = "# Guide\n\n## Overview\n\nunchanged\n\n## Advanced settings\n\nupdated";
        String unchangedInput = "Guide > Overview\n\nunchanged";
        String updatedInput = "Guide > Advanced settings\n\nupdated";
        long id = MarkdownIndexService.entityId(ROOT, "sk/guide.md", 0);
        when(chunker.chunkWithOffsets(markdown)).thenReturn(List.of(
            new Chunk(markdown.indexOf("unchanged"), "unchanged"), new Chunk(markdown.indexOf("updated"), "updated")));
        when(store.getExistingEmbeddingsByHash("MARKDOWN", id, "local", "model", 0))
            .thenReturn(Map.of(MarkdownIndexService.hash(unchangedInput), reused,
                MarkdownIndexService.hash("Guide > Settings\n\nupdated"), new float[] {1, 1}));
        when(embeddings.embedWithUsage(List.of(updatedInput), assistant, "billing.example", EmbeddingInputType.DOCUMENT))
            .thenReturn(new EmbeddingBatchResult(List.of(updated), 7));

        service.indexFile(ROOT, "sk/guide.md", markdown, assistant, List.of(), "billing.example");

        verify(embeddings).embedWithUsage(List.of(updatedInput), assistant, "billing.example", EmbeddingInputType.DOCUMENT);
        verify(statistics).recordIndexingTokens(assistant, 7, 2);
        verify(repository).saveAllAndFlush(savedChunks.capture());
        assertEquals(List.of(unchangedInput, updatedInput), savedChunks.getValue().stream().map(EmbeddingChunkEntity::getChunkText).toList());
        assertEquals(List.of(MarkdownIndexService.hash(unchangedInput), MarkdownIndexService.hash(updatedInput)),
            savedChunks.getValue().stream().map(EmbeddingChunkEntity::getContentHash).toList());
        EmbeddingChunkEntity row = savedChunks.getValue().get(0);
        assertEquals(ROOT + "/sk/guide.md", row.getSourcePath());
        assertEquals("Guide", row.getSourceTitle());
        assertEquals("sk", row.getLanguage());
        assertEquals(0, row.getDomainId());
        assertEquals(MarkdownIndexService.hash(markdown), row.getSourceHash());
        verify(store).updateEmbeddingBatch(List.of(1L, 2L), List.of(reused, updated));
    }

    /** Preserves stored chunks when the embedding provider fails. */
    @Test
    void providerFailureKeepsPreviousIndex() throws Exception {
        when(chunker.chunkWithOffsets("changed")).thenReturn(List.of(new Chunk(0, "changed")));
        when(embeddings.embedWithUsage(anyList(), eq(assistant), anyString(), any()))
            .thenThrow(new IllegalStateException("Provider unavailable"));
        assertThrows(IllegalStateException.class,
            () -> service.indexFile(ROOT, "sk/guide.md", "changed", assistant, List.of(), "billing.example"));
        verifyNoInteractions(repository);
    }

    /** Restores missing stored context using cached vectors, then skips the fully updated source. */
    @Test
    void updatesStoredContextAndSkipsUnchangedSource() throws Exception {
        String body = "Enable this option.";
        String markdown = "# Guide\n\n## Settings\n\n" + body;
        String contextual = "Guide > Settings\n\n" + body;
        EmbeddingChunkEntity existing = new EmbeddingChunkEntity();
        existing.setChunkIndex(0);
        existing.setEmbeddingProvider("local");
        existing.setEmbeddingModel("model");
        existing.setSourceHash(MarkdownIndexService.hash(markdown));
        existing.setContentHash(MarkdownIndexService.hash(contextual));
        existing.setChunkText(body);
        existing.setDimensions(2);
        existing.setLanguage("en");
        existing.setStatus(EmbeddingChunkStatus.COMPLETED);
        when(chunker.chunkWithOffsets(markdown)).thenReturn(List.of(new Chunk(markdown.indexOf(body), body)));
        long id = MarkdownIndexService.entityId(ROOT, "en/guide.md", 0);
        when(store.getExistingEmbeddingsByHash("MARKDOWN", id, "local", "model", 0))
            .thenReturn(Map.of(existing.getContentHash(), new float[] {1, 0}));

        service.indexFile(ROOT, "en/guide.md", markdown, assistant, List.of(existing), "billing.example");

        verify(repository).saveAllAndFlush(savedChunks.capture());
        EmbeddingChunkEntity updated = savedChunks.getValue().get(0);
        assertEquals(contextual, updated.getChunkText());
        updated.setStatus(EmbeddingChunkStatus.COMPLETED);
        clearInvocations(repository, store);

        service.indexFile(ROOT, "en/guide.md", markdown, assistant, List.of(updated), "billing.example");

        verify(embeddings, never()).embedWithUsage(anyList(), any(), anyString(), any());
        verifyNoInteractions(statistics, repository, store);
    }

    /** Deletes stale chunks without calling the embedding provider when cleanup leaves no content. */
    @Test
    void removesChunksForNoiseOnlySources() throws Exception {
        when(chunker.chunkWithOffsets("")).thenReturn(List.of());
        long id = MarkdownIndexService.entityId(ROOT, "en/empty.md", 0);

        service.indexFile(ROOT, "en/empty.md", "![](screen.png)\n\n<!-- internal -->", assistant, List.of(), "billing.example");

        verify(repository).deleteByEntityTypeAndEntityIdAndDomainId(RagEntityType.MARKDOWN, id, 0);
        verifyNoInteractions(embeddings, statistics, store);
    }
}
