package sk.iway.iwcm.rag.vectorstore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver.Resolution;

class VectorStoreRouterTest {

    @Test
    void delegatesPostgreSqlSearchToPgVectorStore() {
        PgVectorStore pgVectorStore = mock(PgVectorStore.class);
        MariaDbVectorStore mariaDbVectorStore = mock(MariaDbVectorStore.class);
        VectorStoreRouter router = new VectorStoreRouter(pgVectorStore, mariaDbVectorStore);
        float[] embedding = { 1.0f, 2.0f };
        List<VectorSearchResult> expected = List.of(mock(VectorSearchResult.class));
        when(pgVectorStore.search(embedding, "openai", "model", RagEntityType.DOCUMENT, 1, "sk", 5, Map.of()))
            .thenReturn(expected);

        try (MockedStatic<VectorStoreDataSourceResolver> resolver = mockStatic(VectorStoreDataSourceResolver.class)) {
            resolver.when(VectorStoreDataSourceResolver::resolve).thenReturn(resolution(VectorStoreType.POSTGRESQL));

            List<VectorSearchResult> actual = router.search(
                embedding, "openai", "model", RagEntityType.DOCUMENT, 1, "sk", 5, Map.of()
            );

            assertSame(expected, actual);
            verify(mariaDbVectorStore, never()).search(any(), any(), any(), any(), any(), any(), anyInt(), any());
        }
    }

    @Test
    void delegatesMariaDbIndexRecreation() {
        PgVectorStore pgVectorStore = mock(PgVectorStore.class);
        MariaDbVectorStore mariaDbVectorStore = mock(MariaDbVectorStore.class);
        VectorStoreRouter router = new VectorStoreRouter(pgVectorStore, mariaDbVectorStore);
        when(mariaDbVectorStore.recreateIndex()).thenReturn(true);

        try (MockedStatic<VectorStoreDataSourceResolver> resolver = mockStatic(VectorStoreDataSourceResolver.class)) {
            resolver.when(VectorStoreDataSourceResolver::resolve).thenReturn(resolution(VectorStoreType.MARIADB));

            assertTrue(router.recreateIndex());
            verify(mariaDbVectorStore).recreateIndex();
            verify(pgVectorStore, never()).recreateIndex();
        }
    }

    @Test
    void unsupportedBackendReturnsSafeResults() {
        PgVectorStore pgVectorStore = mock(PgVectorStore.class);
        MariaDbVectorStore mariaDbVectorStore = mock(MariaDbVectorStore.class);
        VectorStoreRouter router = new VectorStoreRouter(pgVectorStore, mariaDbVectorStore);
        Resolution unsupported = new Resolution(
            "rag_jpa", VectorStoreType.UNSUPPORTED, "MySQL", "8.4", "Unsupported RAG database product: MySQL", true
        );

        try (MockedStatic<VectorStoreDataSourceResolver> resolver = mockStatic(VectorStoreDataSourceResolver.class);
             MockedStatic<Logger> ignored = mockStatic(Logger.class)) {
            resolver.when(VectorStoreDataSourceResolver::resolve).thenReturn(unsupported);
            resolver.when(VectorStoreDataSourceResolver::isRagAvailable).thenReturn(false);

            assertFalse(router.isAvailable());
            assertFalse(router.initializeSchema());
            assertTrue(router.search(new float[] { 1.0f }, "openai", "model", null, null, null, 5, Map.of()).isEmpty());
            assertTrue(router.getExistingEmbeddingsByHash("document", 1L, "openai", "model", 1).isEmpty());
        }
    }

    private static Resolution resolution(VectorStoreType backend) {
        return new Resolution("iwcm", backend, backend.name(), "test", null, false);
    }
}
