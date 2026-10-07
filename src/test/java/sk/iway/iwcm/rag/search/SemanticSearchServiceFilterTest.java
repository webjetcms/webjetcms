package sk.iway.iwcm.rag.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.webjetcms.ai.EmbeddingInputType;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.components.ai.jpa.AssistantDefinitionEntity;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.rag.embedding.EmbeddingBatchResult;
import sk.iway.iwcm.rag.embedding.EmbeddingService;
import sk.iway.iwcm.rag.service.RagEmbeddingStatService;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.VectorSearchResult;
import sk.iway.iwcm.rag.vectorstore.VectorStore;
import sk.iway.iwcm.test.BaseWebjetTest;
import sk.iway.iwcm.test.TestRequest;

/**
 * Tests assistant-based retrieval, shared Markdown accounting, similarity filtering, and reciprocal rank fusion.
 */
class SemanticSearchServiceFilterTest extends BaseWebjetTest {

    /** Uses shared Markdown storage while the requesting domain owns embedding usage. */
    @Test
    void markdownSearchUsesSharedStorageAndCallerAccounting() throws Exception {
        boolean originalHybridEnabled = Constants.getBoolean("ragHybridSearchEnabled");
        RequestBean previous = SetCharacterEncodingFilter.getCurrentRequestBean();
        RequestBean caller = new RequestBean();
        caller.setDomain("tenant.example");
        SetCharacterEncodingFilter.setCurrentRequestBean(caller);
        Constants.setBoolean("ragHybridSearchEnabled", false);
        try {
            EmbeddingService embeddings = mock(EmbeddingService.class);
            VectorStore vectors = mock(VectorStore.class);
            RagEmbeddingStatService statistics = mock(RagEmbeddingStatService.class);
            AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
            assistant.setProvider("openai");
            assistant.setModel("embedding-model");
            TestRequest request = new TestRequest();
            when(vectors.isAvailableAndInitialized()).thenReturn(true);
            when(vectors.search(any(float[].class), any(), any(), any(), any(), any(), anyInt(), any())).thenAnswer(call -> {
                assertNull(SetCharacterEncodingFilter.getCurrentRequestBean().getDomain());
                return List.of();
            });
            when(statistics.getSearchAssistant(7)).thenAnswer(call -> {
                assertSame(caller, SetCharacterEncodingFilter.getCurrentRequestBean());
                return assistant;
            });
            when(embeddings.embedWithUsage(List.of("query"), assistant, request, EmbeddingInputType.QUERY))
                .thenAnswer(call -> {
                    assertSame(caller, SetCharacterEncodingFilter.getCurrentRequestBean());
                    return new EmbeddingBatchResult(List.of(new float[] {1f, 2f}), 2);
                });
            SemanticSearchService service = new SemanticSearchService(embeddings, vectors, statistics, mock(RagService.class), null);
            Map<String, Object> filters = Map.of("sourceRoot", "/docs/webjetcms");

            service.searchChunks("query", 7, "sk", 10, RagEntityType.MARKDOWN, filters, request);

            verify(vectors).search(any(float[].class), eq("openai"), eq("embedding-model"),
                eq(RagEntityType.MARKDOWN), eq(0), eq("sk"), anyInt(), eq(filters));
            verify(statistics).recordSearchTokens(assistant, 2, 7);
            assertSame(caller, SetCharacterEncodingFilter.getCurrentRequestBean());
        } finally {
            SetCharacterEncodingFilter.setCurrentRequestBean(previous);
            Constants.setBoolean("ragHybridSearchEnabled", originalHybridEnabled);
        }
    }

    /**
     * Verifies that query embeddings and retrieval use the existing search assistant, with a normalized provider identifier.
     */
    @Test
    void searchUsesProviderAndModelFromExistingAssistant() throws Exception {
        boolean originalHybridEnabled = Constants.getBoolean("ragHybridSearchEnabled");
        boolean originalAnswerAllowed = Constants.getBoolean("ragAnswerAllowed");
        Constants.setBoolean("ragHybridSearchEnabled", false);
        Constants.setBoolean("ragAnswerAllowed", false);

        try (var documents = mockStatic(DocDB.class); var groups = mockStatic(GroupsDB.class)) {
            EmbeddingService embeddingService = mock(EmbeddingService.class);
            VectorStore vectorStore = mock(VectorStore.class);
            RagEmbeddingStatService statService = mock(RagEmbeddingStatService.class);
            AssistantDefinitionEntity assistant = new AssistantDefinitionEntity();
            assistant.setProvider("GEMINI");
            assistant.setModel("gemini-embedding-001");
            TestRequest request = new TestRequest();
            request.setAttribute("rootGroup", "");

            when(vectorStore.isAvailableAndInitialized()).thenReturn(true);
            when(statService.getSearchAssistant()).thenReturn(assistant);
            when(embeddingService.embedWithUsage(List.of("query"), assistant, request, EmbeddingInputType.QUERY))
                .thenReturn(new EmbeddingBatchResult(List.of(new float[] {1f, 2f}), 3));
            DocDB docDb = mock(DocDB.class);
            documents.when(DocDB::getInstance).thenReturn(docDb);
            groups.when(GroupsDB::getInstance).thenReturn(mock(GroupsDB.class));
            for (int id = 1; id <= 2; id++) {
                DocDetails doc = new DocDetails();
                doc.setAvailable(true);
                doc.setSearchable(true);
                when(docDb.getBasicDocDetails(id, false)).thenReturn(doc);
                boolean allowed = id == 1;
                documents.when(() -> DocDB.canAccess(doc, null, true)).thenReturn(allowed);
            }
            var hits = List.of(
                new VectorSearchResult(1L, "document", 1L, 0, "Answer", .75),
                new VectorSearchResult(2L, "document", 1L, 1, "Overview", .8),
                new VectorSearchResult(3L, "document", 2L, 0, "Denied", .9));
            when(vectorStore.search(any(), any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(hits);
            RerankerService reranker = mock(RerankerService.class);
            hits.get(0).setRerankScore(.7875);
            hits.get(1).setRerankScore(.68);
            when(reranker.rerank("query", hits.subList(0, 2))).thenReturn(hits.subList(0, 2));
            SemanticSearchService service = new SemanticSearchService(embeddingService, vectorStore, statService, mock(RagService.class), reranker);

            var results = service.search("query", 1, "sk", 10, RagEntityType.DOCUMENT, request);

            assertEquals(List.of(1L), results.stream().map(SemanticSearchResult::getDocId).toList());
            assertEquals(.8, results.get(0).getSimilarity());
            assertEquals(.7875, results.get(0).getRankingScore());
            assertEquals(results, service.filterResultsBySimilarity(results, .79, 0));
            verify(reranker).rerank("query", hits.subList(0, 2));

            verify(embeddingService).embedWithUsage(List.of("query"), assistant, request, EmbeddingInputType.QUERY);
            verify(vectorStore).search(
                any(float[].class),
                eq("gemini"),
                eq("gemini-embedding-001"),
                eq(RagEntityType.DOCUMENT),
                eq(1),
                eq("sk"),
                anyInt(),
                isNull()
            );
        } finally {
            Constants.setBoolean("ragHybridSearchEnabled", originalHybridEnabled);
            Constants.setBoolean("ragAnswerAllowed", originalAnswerAllowed);
        }
    }

    /**
     * Verifies that the highest remaining score fills the minimum result count when the adaptive threshold retains too few results.
     */
    @Test
    void filterResultsBySimilarityAddsFallbackWhenThresholdKeepsTooFew() {
        SemanticSearchService service = new SemanticSearchService(null, null, null, null, null);

        List<SemanticSearchResult> results = List.of(
            new SemanticSearchResult(1L, 0.90),
            new SemanticSearchResult(2L, 0.80),
            new SemanticSearchResult(3L, 0.60),
            new SemanticSearchResult(4L, 0.50)
        );

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(results, 0.2d, 3);

        assertEquals(3, filtered.size());
        assertEquals(1L, filtered.get(0).getDocId());
        assertEquals(2L, filtered.get(1).getDocId());
        assertEquals(3L, filtered.get(2).getDocId());
    }

    /**
     * Verifies that all results above the adaptive threshold remain even when their count exceeds the configured minimum.
     */
    @Test
    void filterResultsBySimilarityKeepsAllStrongResults() {
        SemanticSearchService service = new SemanticSearchService(null, null, null, null, null);

        List<SemanticSearchResult> results = List.of(
            new SemanticSearchResult(1L, 0.95),
            new SemanticSearchResult(2L, 0.90),
            new SemanticSearchResult(3L, 0.80),
            new SemanticSearchResult(4L, 0.75)
        );

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(results, 0.2d, 2);

        assertEquals(4, filtered.size());
        assertEquals(1L, filtered.get(0).getDocId());
        assertEquals(4L, filtered.get(3).getDocId());
    }

    /**
     * Verifies that the best three results are retained as fallback when every score is below the configured floor.
     */
    @Test
    void filterResultsBySimilarityUsesConfiguredFloorWhenTopSimilarityIsLow() {
        SemanticSearchService service = new SemanticSearchService(null, null, null, null, null);

        List<SemanticSearchResult> results = List.of(
            new SemanticSearchResult(1L, 0.25),
            new SemanticSearchResult(2L, 0.20),
            new SemanticSearchResult(3L, 0.15),
            new SemanticSearchResult(4L, 0.10)
        );

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(results, 0.4d, 3);

        assertEquals(3, filtered.size());
        assertEquals(1L, filtered.get(0).getDocId());
        assertEquals(2L, filtered.get(1).getDocId());
        assertEquals(3L, filtered.get(2).getDocId());
    }

    /**
     * Verifies that empty input produces no results even when a positive minimum count is requested.
     */
    @Test
    void filterResultsBySimilarityReturnsEmptyForEmptyInput() {
        SemanticSearchService service = new SemanticSearchService(null, null, null, null, null);

        List<SemanticSearchResult> filtered = service.filterResultsBySimilarity(List.of(), 0.2d, 3);

        assertEquals(0, filtered.size());
    }

    /**
     * Verifies that reciprocal rank fusion deduplicates chunks and ranks the chunk found by both retrieval sources first.
     */
    @Test
    void mergeChunkResultsWithRrfBoostsChunksPresentInBothSources() {
        SemanticSearchService service = new SemanticSearchService(null, null, null, null, null);

        List<VectorSearchResult> vectorResults = List.of(
            new VectorSearchResult(1L, "document", 10L, 0, "vector-1", 0.90),
            new VectorSearchResult(2L, "document", 20L, 0, "vector-2", 0.80)
        );

        List<VectorSearchResult> fulltextResults = List.of(
            new VectorSearchResult(2L, "document", 20L, 0, "fts-2", 0.95),
            new VectorSearchResult(3L, "document", 30L, 0, "fts-3", 0.70)
        );

        List<VectorSearchResult> merged = service.mergeChunkResultsWithRrf(vectorResults, fulltextResults, null);

        assertEquals(3, merged.size());
        assertEquals(2L, merged.get(0).getChunkId());
    }
}
