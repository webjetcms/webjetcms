package sk.iway.iwcm.rag.vectorstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.rag.vectorstore.VectorStoreDataSourceResolver.Resolution;

/** Verifies database-level isolation of Markdown collections before result limiting. */
class MarkdownVectorScopeTest {

    /** Both backends ignore missing query vectors and nonpositive limits before running SQL. */
    @ParameterizedTest
    @CsvSource({"POSTGRESQL,-1,4", "POSTGRESQL,0,4", "POSTGRESQL,1,0", "POSTGRESQL,1,-1",
        "MARIADB,-1,4", "MARIADB,0,4", "MARIADB,1,0", "MARIADB,1,-1"})
    void skipsInvalidVectorInput(VectorStoreType backend, int dimensions, int limit) {
        try (var pools = mockStatic(DBPool.class);
             var resolver = mockStatic(VectorStoreDataSourceResolver.class)) {
            resolver.when(VectorStoreDataSourceResolver::resolve).thenReturn(
                new Resolution("rag_jpa", backend, backend.name(), "test", null, true));
            VectorStore store = backend == VectorStoreType.POSTGRESQL ? new PgVectorStore() : new MariaDbVectorStore();

            assertTrue(store.search(dimensions < 0 ? null : new float[dimensions], "openai", "model",
                RagEntityType.MARKDOWN, 0, "sk", limit, null).isEmpty());
            pools.verifyNoInteractions();
        }
    }

    /** Domain zero is an exact shared-domain filter in vector, full-text, and fallback retrieval on both backends. */
    @ParameterizedTest
    @CsvSource({"POSTGRESQL,vector,false", "POSTGRESQL,fulltext,false", "POSTGRESQL,fallback,false",
        "MARIADB,vector,false", "MARIADB,fulltext,false", "MARIADB,fallback,false",
        "POSTGRESQL,vector,true", "POSTGRESQL,fulltext,true", "POSTGRESQL,fallback,true",
        "MARIADB,vector,true", "MARIADB,fulltext,true", "MARIADB,fallback,true"})
    void bindsCollectionDomainAndLanguageBeforeLimit(VectorStoreType backend, String mode, boolean multipleRoots) throws Exception {
        Connection connection = mock(Connection.class);
        List<String> queries = new ArrayList<>();
        List<Map<Integer, Object>> parameters = new ArrayList<>();
        when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
            queries.add(invocation.getArgument(0));
            Map<Integer, Object> bound = new HashMap<>();
            parameters.add(bound);
            PreparedStatement statement = mock(PreparedStatement.class);
            doAnswer(binding -> {
                bound.put(binding.getArgument(0), binding.getArgument(1));
                return null;
            }).when(statement).setObject(anyInt(), any());
            when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
            return statement;
        });

        String sourceRoot = "/admin/docs/100%_done!/client' OR 1=1 --";
        String sourcePathPattern = "/admin/docs/100!%!_done!!/client' OR 1=1 --/%";
        Map<String, Object> bonusParams = Map.of(
            multipleRoots ? "sourceRoots" : "sourceRoot", multipleRoots ? List.of(sourceRoot, "file:/docs/sk/redactor") : sourceRoot,
            "hybridFtsUseIlikeFallback", "fallback".equals(mode)
        );
        try (MockedStatic<Constants> constants = mockStatic(Constants.class);
             MockedStatic<DBPool> pools = mockStatic(DBPool.class);
             MockedStatic<Logger> logger = mockStatic(Logger.class);
             MockedStatic<VectorStoreDataSourceResolver> resolver = mockStatic(VectorStoreDataSourceResolver.class)) {
            constants.when(() -> Constants.getString("ragSearchDistanceMetric")).thenReturn("cosine");
            constants.when(() -> Constants.getInt("ragSearchEfSearch")).thenReturn(40);
            pools.when(() -> DBPool.getConnection("rag_jpa")).thenReturn(connection);
            resolver.when(VectorStoreDataSourceResolver::resolve).thenReturn(
                new Resolution("rag_jpa", backend, backend.name(), "test", null, true)
            );

            VectorStore store = backend == VectorStoreType.POSTGRESQL ? new PgVectorStore() : new MariaDbVectorStore();
            if ("vector".equals(mode)) {
                store.search(new float[] {0.1f, 0.2f}, "openai", "model", RagEntityType.MARKDOWN, 0, "sk", 4, bonusParams);
            } else {
                store.searchFulltext("documentation", "openai", "model", RagEntityType.MARKDOWN, 0, "sk", 4, bonusParams);
            }
        }

        assertEquals("fallback".equals(mode) ? 2 : 1, queries.size());
        for (int i = 0; i < queries.size(); i++) {
            String sql = queries.get(i);
            assertTrue(sql.indexOf("source_path LIKE ") >= 0);
            assertTrue(sql.indexOf("source_path LIKE ") < sql.indexOf("LIMIT ?"));
            assertTrue(sql.contains("? ESCAPE '!'"));
            if (multipleRoots) {
                assertTrue(sql.contains(" OR "));
                assertTrue(parameters.get(i).containsValue("file:/docs/sk/redactor/%"));
            }
            assertFalse(sql.contains(sourceRoot));
            assertTrue(sql.contains("entity_type = ?"));
            assertTrue(sql.contains("domain_id = ?"));
            assertTrue(sql.contains("language = ?"));
            assertTrue(parameters.get(i).values().containsAll(List.of(sourcePathPattern, "MARKDOWN", 0, "sk")));
            if (backend == VectorStoreType.MARIADB) {
                assertTrue(sql.contains("source_path LIKE BINARY ?"));
            }
        }
    }
}
