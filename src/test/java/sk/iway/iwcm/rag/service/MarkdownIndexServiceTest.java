package sk.iway.iwcm.rag.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.webjetcms.ai.EmbeddingInputType;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.indexing.MarkdownContentExtractor;
import sk.iway.iwcm.rag.indexing.SlidingWindowChunker;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkEntity;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkRepository;
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
    private final MarkdownIndexService service = new MarkdownIndexService(new MarkdownSourceService(),
        new MarkdownContentExtractor(), chunker, embeddings, statistics, repository, store);

    @BeforeEach
    void setup() {
        assistant.setProvider("local");
        assistant.setDomainId(2);
        assistant.setModel("model");
        when(embeddings.getDimensions()).thenReturn(2);
        when(chunker.chunk(anyString())).thenReturn(List.of("unchanged", "updated"));
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
        when(store.getExistingEmbeddingsByHash("MARKDOWN", id, "local", "model", 0))
            .thenReturn(Map.of(MarkdownIndexService.hash("unchanged"), reused));
        when(embeddings.embedWithUsage(List.of("updated"), assistant, "billing.example", EmbeddingInputType.DOCUMENT))
            .thenReturn(new EmbeddingBatchResult(List.of(updated), 7));

        service.indexFile(ROOT, "sk/guide.md", "# Guide", assistant, List.of(), "billing.example");

        verify(embeddings).embedWithUsage(List.of("updated"), assistant, "billing.example", EmbeddingInputType.DOCUMENT);
        verify(statistics).recordIndexingTokens(assistant, 7, 2);
        verify(repository).saveAllAndFlush(argThat(rows -> {
            EmbeddingChunkEntity row = rows.iterator().next();
            return (ROOT + "/sk/guide.md").equals(row.getSourcePath()) && "Guide".equals(row.getSourceTitle())
                && "sk".equals(row.getLanguage()) && row.getDomainId() == 0;
        }));
        verify(store).updateEmbeddingBatch(List.of(1L, 2L), List.of(reused, updated));
    }

    /**
     * Verifies that an embedding provider failure propagates before any stored chunks are replaced.
     */
    @Test
    void providerFailureKeepsPreviousIndex() throws Exception {
        when(embeddings.embedWithUsage(anyList(), eq(assistant), anyString(), any()))
            .thenThrow(new IllegalStateException("Provider unavailable"));
        assertThrows(IllegalStateException.class,
            () -> service.indexFile(ROOT, "sk/guide.md", "changed", assistant, List.of(), "billing.example"));
        verifyNoInteractions(repository);
    }
}
