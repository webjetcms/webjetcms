package sk.iway.iwcm.rag.vectorstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.service.RagEntityType;

/**
 * MariaDB implementation of {@link VectorStore} using the native VECTOR type.
 * Chunk metadata remains in {@code rag_embedding_chunks}; non-null vectors are
 * stored separately so failed and pending chunks can remain vectorless.
 */
@Service
public class MariaDbVectorStore implements VectorStore {

    static final String VECTOR_INDEX_NAME = "idx_rag_embedding_vector";

    private static final String CHUNK_TABLE_NAME = "rag_embedding_chunks";
    private static final String VECTOR_TABLE_NAME = "rag_embedding_vectors";
    private static final String UNIQUE_CHUNK_INDEX_NAME = "uq_rag_chunk";
    private static final String ENTITY_INDEX_NAME = "idx_rag_chunk_entity";
    private static final String DOMAIN_LANG_INDEX_NAME = "idx_rag_chunk_domain_lang";
    private static final String CHUNK_TEXT_FTS_INDEX_NAME = "idx_rag_chunk_text_fts";
    private static final String VECTOR_FOREIGN_KEY_NAME = "fk_rag_embedding_vector_chunk";
    private static final String SCHEMA_LOCK_NAME = "webjet_rag_vector_schema";
    private static final int SCHEMA_LOCK_TIMEOUT_SECONDS = 30;

    private static final int MAX_VECTOR_DIMENSIONS = 16_383;
    private static final int MIN_EF_SEARCH = 1;
    private static final int MAX_EF_SEARCH = 10_000;

    private static final String CREATE_CHUNK_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS rag_embedding_chunks (
            id                 BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
            entity_type        VARCHAR(100) NOT NULL,
            entity_id          BIGINT NOT NULL,
            chunk_index        INT NOT NULL,
            chunk_text         TEXT NOT NULL,
            content_hash       VARCHAR(64) NOT NULL,
            source_path        TEXT,
            source_title       VARCHAR(512),
            source_hash        VARCHAR(64),
            embedding_provider VARCHAR(100) NOT NULL,
            embedding_model    VARCHAR(100) NOT NULL,
            dimensions         INT NOT NULL,
            language           VARCHAR(10),
            domain_id          INT,
            group_id           INT,
            root_group_l1      INT,
            root_group_l2      INT,
            root_group_l3      INT,
            status             VARCHAR(20) NOT NULL DEFAULT '%s',
            error_message      VARCHAR(500),
            create_date        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT uq_rag_chunk UNIQUE
                (entity_type, entity_id, chunk_index, embedding_provider, embedding_model)
        ) ENGINE=InnoDB
        """.formatted(EmbeddingChunkStatus.PENDING.name());

    private static final String CREATE_VECTOR_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS rag_embedding_vectors (
            chunk_id  BIGINT NOT NULL PRIMARY KEY,
            embedding VECTOR(%d) NOT NULL,
            CONSTRAINT fk_rag_embedding_vector_chunk
                FOREIGN KEY (chunk_id) REFERENCES rag_embedding_chunks(id) ON DELETE CASCADE
        ) ENGINE=InnoDB
        """;

    private static final String UPSERT_EMBEDDING_SQL = """
        INSERT INTO rag_embedding_vectors (chunk_id, embedding)
        VALUES (?, VEC_FromText(?))
        ON DUPLICATE KEY UPDATE embedding = VALUES(embedding)
        """;

    private static final String MARK_COMPLETED_SQL =
        "UPDATE rag_embedding_chunks SET status = '" + EmbeddingChunkStatus.COMPLETED.name() +
        "', error_message = NULL WHERE id = ?";

    private static final String MARK_ERROR_SQL =
        "UPDATE rag_embedding_chunks SET status = '" + EmbeddingChunkStatus.ERROR.name() +
        "', error_message = ? WHERE id = ?";

    private static final String GET_EXISTING_HASH_EMBEDDING_SQL = """
        SELECT c.content_hash, VEC_ToText(v.embedding) AS embedding_text
        FROM rag_embedding_chunks c
        JOIN rag_embedding_vectors v ON v.chunk_id = c.id
        WHERE c.entity_type = ?
          AND c.entity_id = ?
          AND c.embedding_provider = ?
          AND c.embedding_model = ?
          AND c.domain_id = ?
          AND c.status = '%s'
        """.formatted(EmbeddingChunkStatus.COMPLETED.name());

    @Override
    public void updateEmbedding(Long id, float[] embedding) {
        if (id == null || embedding == null || embedding.length == 0) return;

        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) return;

        updateEmbeddingRow(dataSourceName, id, embedding);
    }

    /**
     * Stores one vector and marks its chunk completed, recording an error status when the update fails.
     *
     * @param dataSourceName resolved RAG datasource name
     * @param id existing chunk ID
     * @param embedding nonempty embedding vector
     * @return {@code true} when the vector and completed status were stored successfully
     */
    private boolean updateEmbeddingRow(String dataSourceName, Long id, float[] embedding) {
        try (Connection connection = getConnection(dataSourceName)) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                upsertEmbedding(connection, id, embedding);
                executeUpdate(connection, MARK_COMPLETED_SQL, id);
                connection.commit();
                return true;
            } catch (Exception e) {
                rollback(connection, e);
                throw e;
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Error updating MariaDB embedding for chunk id " + id + ": " + e.getMessage());
            markEmbeddingError(dataSourceName, id, e.getMessage());
            return false;
        }
    }

    @Override
    public void updateEmbeddingBatch(List<Long> ids, List<float[]> embeddings) {
        if (ids == null || ids.isEmpty() || embeddings == null || embeddings.isEmpty()) return;
        if (ids.size() != embeddings.size()) {
            throw new IllegalArgumentException(
                "IDs/embeddings count mismatch: ids=" + ids.size() + ", embeddings=" + embeddings.size());
        }

        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) return;

        try {
            updateEmbeddingBatchTransaction(dataSourceName, ids, embeddings);
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Error batch updating MariaDB embeddings, falling back to row-by-row updates: " + e.getMessage());
            int failedRows = 0;
            for (int i = 0; i < ids.size(); i++) {
                if (updateEmbeddingRow(dataSourceName, ids.get(i), embeddings.get(i)) == false) {
                    failedRows++;
                }
            }
            if (failedRows > 0) {
                throw new IllegalStateException(
                    "Failed to update " + failedRows + " of " + ids.size() + " embedding rows", e);
            }
        }
    }

    /**
     * Stores vectors and completed statuses in one transaction, rolling back the batch on failure.
     *
     * @param dataSourceName resolved RAG datasource name
     * @param ids existing chunk IDs in vector order
     * @param embeddings nonempty vectors corresponding to the IDs
     * @throws SQLException if a row is invalid, a database operation fails, or batch completion cannot be verified
     */
    private void updateEmbeddingBatchTransaction(
        String dataSourceName,
        List<Long> ids,
        List<float[]> embeddings
    ) throws SQLException {
        try (Connection connection = getConnection(dataSourceName)) {
            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement vectorStatement = connection.prepareStatement(UPSERT_EMBEDDING_SQL);
                     PreparedStatement statusStatement = connection.prepareStatement(MARK_COMPLETED_SQL)) {
                    for (int i = 0; i < ids.size(); i++) {
                        Long id = ids.get(i);
                        float[] embedding = embeddings.get(i);
                        if (id == null || embedding == null || embedding.length == 0) {
                            throw new IllegalArgumentException("Chunk ID and embedding must not be empty at index " + i);
                        }

                        vectorStatement.setLong(1, id);
                        vectorStatement.setString(2, vectorToString(embedding));
                        vectorStatement.addBatch();

                        statusStatement.setLong(1, id);
                        statusStatement.addBatch();
                    }

                    assertBatchSuccessful("vector", vectorStatement.executeBatch(), ids.size());
                    assertBatchSuccessful("status", statusStatement.executeBatch(), ids.size());
                }
                connection.commit();
            } catch (Exception e) {
                rollback(connection, e);
                if (e instanceof SQLException sqlException) throw sqlException;
                throw new SQLException(e.getMessage(), e);
            } finally {
                restoreAutoCommit(connection, originalAutoCommit);
            }
        }
    }

    private void upsertEmbedding(Connection connection, Long id, float[] embedding) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_EMBEDDING_SQL)) {
            statement.setLong(1, id);
            statement.setString(2, vectorToString(embedding));
            statement.executeUpdate();
        }
    }

    /**
     * Checks that a JDBC batch reports the expected number of results without failed statements.
     *
     * @param operation operation name used in error messages
     * @param updateCounts JDBC batch update counts
     * @param expectedRows expected number of batch results
     * @throws IllegalStateException if the result count differs or any statement reports failure
     */
    private void assertBatchSuccessful(String operation, int[] updateCounts, int expectedRows) {
        if (updateCounts.length != expectedRows) {
            throw new IllegalStateException(
                "Expected " + expectedRows + " " + operation + " batch results, received " + updateCounts.length);
        }
        for (int updateCount : updateCounts) {
            if (updateCount == Statement.EXECUTE_FAILED) {
                throw new IllegalStateException("MariaDB " + operation + " batch update failed");
            }
        }
    }

    /**
     * Marks a failed vector update with a bounded error message and logs status-update failures.
     *
     * @param dataSourceName resolved RAG datasource name
     * @param id chunk ID to mark as failed
     * @param errorMessage failure message, or empty for the default message
     */
    private void markEmbeddingError(String dataSourceName, Long id, String errorMessage) {
        String message = Tools.isEmpty(errorMessage) ? "Embedding vector update failed" : errorMessage;
        if (message.length() > 500) message = message.substring(0, 500);

        try (Connection connection = getConnection(dataSourceName)) {
            int updatedRows = executeUpdate(connection, MARK_ERROR_SQL, message, id);
            if (updatedRows != 1) {
                Logger.error(MariaDbVectorStore.class,
                    "Failed to mark embedding chunk " + id + " as ERROR, updated rows: " + updatedRows);
            }
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Failed to mark embedding chunk " + id + " as ERROR: " + e.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> search(
        float[] queryEmbedding,
        String embeddingProvider,
        String embeddingModel,
        RagEntityType entityType,
        Integer domainId,
        String language,
        int limit,
        Map<String, Object> bonusParams
    ) {
        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null || queryEmbedding == null || queryEmbedding.length == 0 || limit <= 0) {
            return new ArrayList<>();
        }

        String metric = configuredMetric();
        if (isSupportedMetric(metric) == false) {
            logUnsupportedMetric(metric);
            return new ArrayList<>();
        }

        StringBuilder conditions = new StringBuilder();
        List<Object> filterParams = new ArrayList<>();
        conditions.append(" AND c.embedding_provider = ? AND c.embedding_model = ?");
        filterParams.add(embeddingProvider);
        filterParams.add(embeddingModel);
        addScopeFilters(conditions, filterParams, "c.", entityType, domainId, language);
        addEntityTypeSpecificConditions(conditions, filterParams, "c.", entityType, bonusParams);

        int efSearch = Constants.getInt("ragSearchEfSearch");
        boolean applyEfSearch = efSearch >= MIN_EF_SEARCH && efSearch <= MAX_EF_SEARCH;
        if (applyEfSearch == false) {
            Logger.warn(MariaDbVectorStore.class,
                "Ignoring invalid MariaDB mhnsw_ef_search value " + efSearch +
                "; expected a value from " + MIN_EF_SEARCH + " to " + MAX_EF_SEARCH);
        }

        String sql = buildVectorSearchSql(metric, conditions.toString(), applyEfSearch ? efSearch : null);
        String vector = vectorToString(queryEmbedding);
        List<Object> params = new ArrayList<>();
        params.add(vector);
        params.addAll(filterParams);
        params.add(vector);
        params.add(limit);

        return executeSearchQuery(dataSourceName, sql, params);
    }

    /**
     * Builds an index-backed nearest-neighbor query with metric-specific similarity conversion.
     *
     * @param metric supported distance metric, {@code cosine} or {@code l2}
     * @param conditions trusted SQL filter fragment with bound-value placeholders
     * @param efSearch validated search-effort override, or {@code null} to use the session value
     * @return SQL requiring the query vector, filter values, query vector again, and result limit
     */
    static String buildVectorSearchSql(String metric, String conditions, Integer efSearch) {
        String distanceFunction = distanceFunction(metric);
        String similarity = "cosine".equals(metric)
            ? "1 - ranked.distance"
            : "1 / (1 + ranked.distance)";
        String statementPrefix = efSearch == null
            ? ""
            : "SET STATEMENT mhnsw_ef_search=" + efSearch + " FOR ";

        return statementPrefix + """
            SELECT ranked.id, ranked.entity_type, ranked.entity_id, ranked.chunk_index,
                   ranked.chunk_text, %s AS similarity
            FROM (
                SELECT c.id, c.entity_type, c.entity_id, c.chunk_index, c.chunk_text,
                       %s(v.embedding, VEC_FromText(?)) AS distance
                FROM rag_embedding_vectors v FORCE INDEX (%s)
                JOIN rag_embedding_chunks c ON c.id = v.chunk_id
                WHERE c.status = '%s'%s
                ORDER BY %s(v.embedding, VEC_FromText(?)) ASC
                LIMIT ?
            ) ranked
            ORDER BY ranked.distance ASC
            """.formatted(
                similarity,
                distanceFunction,
                VECTOR_INDEX_NAME,
                EmbeddingChunkStatus.COMPLETED.name(),
                conditions,
                distanceFunction
            );
    }

    @Override
    public List<VectorSearchResult> searchFulltext(
        String query,
        String embeddingProvider,
        String embeddingModel,
        RagEntityType entityType,
        Integer domainId,
        String language,
        int limit,
        Map<String, Object> bonusParams
    ) {
        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null || Tools.isEmpty(query) || limit <= 0) return new ArrayList<>();

        List<VectorSearchResult> results = executeFulltextSearch(
            dataSourceName,
            query,
            embeddingProvider,
            embeddingModel,
            entityType,
            domainId,
            language,
            limit,
            bonusParams
        );

        boolean useLikeFallback = Constants.getBoolean("ragHybridFtsUseIlikeFallback");
        if (bonusParams != null) {
            useLikeFallback = Tools.getBooleanValue(
                String.valueOf(bonusParams.get("hybridFtsUseIlikeFallback")),
                useLikeFallback
            );
        }

        if (results.isEmpty() && useLikeFallback) {
            return executeLikeSearch(
                dataSourceName,
                query,
                embeddingProvider,
                embeddingModel,
                entityType,
                domainId,
                language,
                limit,
                bonusParams
            );
        }
        return results;
    }

    /**
     * Retrieves completed chunks ranked by native full-text relevance within the requested scope.
     *
     * @param dataSourceName resolved RAG datasource name
     * @param query textual full-text query
     * @param embeddingProvider provider required on matching chunks
     * @param embeddingModel model required on matching chunks
     * @param entityType optional source entity type
     * @param domainId optional exact storage domain, including zero for shared Markdown
     * @param language optional language filter
     * @param limit maximum chunks to return
     * @param bonusParams optional source-root or document-group constraints
     * @return matching chunks in descending relevance order
     */
    private List<VectorSearchResult> executeFulltextSearch(
        String dataSourceName,
        String query,
        String embeddingProvider,
        String embeddingModel,
        RagEntityType entityType,
        Integer domainId,
        String language,
        int limit,
        Map<String, Object> bonusParams
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.id, c.entity_type, c.entity_id, c.chunk_index, c.chunk_text,
                   MATCH(c.chunk_text) AGAINST (? IN NATURAL LANGUAGE MODE) AS similarity
            FROM rag_embedding_chunks c
            WHERE c.status = '%s'
              AND c.embedding_provider = ?
              AND c.embedding_model = ?
              AND MATCH(c.chunk_text) AGAINST (? IN NATURAL LANGUAGE MODE)
            """.formatted(EmbeddingChunkStatus.COMPLETED.name()));
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(embeddingProvider);
        params.add(embeddingModel);
        params.add(query);
        addScopeFilters(sql, params, "c.", entityType, domainId, language);
        addEntityTypeSpecificConditions(sql, params, "c.", entityType, bonusParams);
        sql.append(" ORDER BY similarity DESC, c.id ASC LIMIT ?");
        params.add(limit);
        return executeSearchQuery(dataSourceName, sql.toString(), params);
    }

    /**
     * Retrieves completed chunks using a case-insensitive pattern fallback within the requested scope.
     *
     * @param dataSourceName resolved RAG datasource name
     * @param query query text; SQL pattern wildcards remain active in fallback searches
     * @param embeddingProvider provider required on matching chunks
     * @param embeddingModel model required on matching chunks
     * @param entityType optional source entity type
     * @param domainId optional exact storage domain, including zero for shared Markdown
     * @param language optional language filter
     * @param limit maximum chunks to return
     * @param bonusParams optional source-root or document-group constraints
     * @return matching chunks in descending relevance order
     */
    private List<VectorSearchResult> executeLikeSearch(
        String dataSourceName,
        String query,
        String embeddingProvider,
        String embeddingModel,
        RagEntityType entityType,
        Integer domainId,
        String language,
        int limit,
        Map<String, Object> bonusParams
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT c.id, c.entity_type, c.entity_id, c.chunk_index, c.chunk_text,
                   CASE WHEN LOCATE(LOWER(?), LOWER(c.chunk_text)) > 0 THEN 1.0 ELSE 0.0 END AS similarity
            FROM rag_embedding_chunks c
            WHERE c.status = '%s'
              AND c.embedding_provider = ?
              AND c.embedding_model = ?
              AND LOWER(c.chunk_text) LIKE LOWER(?)
            """.formatted(EmbeddingChunkStatus.COMPLETED.name()));
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(embeddingProvider);
        params.add(embeddingModel);
        params.add("%" + query + "%");
        addScopeFilters(sql, params, "c.", entityType, domainId, language);
        addEntityTypeSpecificConditions(sql, params, "c.", entityType, bonusParams);
        sql.append(" ORDER BY similarity DESC, c.id ASC LIMIT ?");
        params.add(limit);
        return executeSearchQuery(dataSourceName, sql.toString(), params);
    }

    /**
     * Appends optional entity, domain, and language conditions and their bound values.
     *
     * @param sql query builder to extend
     * @param params ordered parameters to extend
     * @param columnPrefix trusted column qualifier, including its trailing dot
     * @param entityType source type, or {@code null} for all types
     * @param domainId exact storage domain, including zero, or {@code null} for all domains
     * @param language language code, or null or empty to omit the filter
     */
    private void addScopeFilters(
        StringBuilder sql,
        List<Object> params,
        String columnPrefix,
        RagEntityType entityType,
        Integer domainId,
        String language
    ) {
        if (entityType != null) {
            sql.append(" AND ").append(columnPrefix).append("entity_type = ?");
            params.add(entityType.name());
        }
        if (domainId != null) {
            sql.append(" AND ").append(columnPrefix).append("domain_id = ?");
            params.add(domainId);
        }
        if (Tools.isNotEmpty(language)) {
            sql.append(" AND ").append(columnPrefix).append("language = ?");
            params.add(language);
        }
    }

    /**
     * Appends a Markdown root-prefix filter or document-group constraints before result limiting.
     * Markdown root wildcards are escaped; document group conditions are combined with OR.
     *
     * @param sql query builder to extend
     * @param params ordered parameters to extend
     * @param columnPrefix trusted column qualifier, including its trailing dot
     * @param entityType source type that selects the applicable filters
     * @param bonusParams optional {@code sourceRoot}, {@code sourceRoots}, or document root-group collections
     */
    private void addEntityTypeSpecificConditions(
        StringBuilder sql,
        List<Object> params,
        String columnPrefix,
        RagEntityType entityType,
        Map<String, Object> bonusParams
    ) {
        if (bonusParams != null && RagEntityType.MARKDOWN == entityType) {
            List<?> roots = bonusParams.get("sourceRoot") instanceof String sourceRoot ? List.of(sourceRoot) :
                bonusParams.get("sourceRoots") instanceof List<?> sourceRoots ? sourceRoots : null;
            if (roots != null) {
                sql.append(" AND (1=0");
                for (Object value : roots) {
                    if (value instanceof String root) {
                        sql.append(" OR ").append(columnPrefix).append("source_path LIKE BINARY ? ESCAPE '!'");
                        params.add(root.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "/%");
                    }
                }
                sql.append(")");
            }
        }
        if (bonusParams == null || RagEntityType.DOCUMENT != entityType) return;

        List<Integer> rootGroupsL1 = asIntegerList(bonusParams.get("rootGroupL1"));
        List<Integer> rootGroupsL2 = asIntegerList(bonusParams.get("rootGroupL2"));
        List<Integer> rootGroupsL3 = asIntegerList(bonusParams.get("rootGroupL3"));
        List<Integer> rootGroups = asIntegerList(bonusParams.get("rootGroups"));

        if (hasValues(rootGroupsL1) == false && hasValues(rootGroupsL2) == false &&
            hasValues(rootGroupsL3) == false && hasValues(rootGroups) == false) {
            return;
        }

        sql.append(" AND (");
        boolean hasPrevious = false;
        hasPrevious = appendInCondition(sql, params, columnPrefix + "root_group_l1", rootGroupsL1, hasPrevious);
        hasPrevious = appendInCondition(sql, params, columnPrefix + "root_group_l2", rootGroupsL2, hasPrevious);
        hasPrevious = appendInCondition(sql, params, columnPrefix + "root_group_l3", rootGroupsL3, hasPrevious);
        appendInCondition(sql, params, columnPrefix + "group_id", rootGroups, hasPrevious);
        sql.append(")");
    }

    /**
     * Appends a parameterized IN clause to a group of OR conditions when values are present.
     *
     * @param sql query builder to extend
     * @param params ordered parameters to extend
     * @param column trusted qualified SQL column name
     * @param values group IDs to include
     * @param hasPrevious whether the condition group already contains a clause
     * @return whether the group contains a clause after this call
     */
    private boolean appendInCondition(
        StringBuilder sql,
        List<Object> params,
        String column,
        List<Integer> values,
        boolean hasPrevious
    ) {
        if (hasValues(values) == false) return hasPrevious;
        if (hasPrevious) sql.append(" OR ");
        sql.append(column).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
            params.add(values.get(i));
        }
        sql.append(")");
        return true;
    }

    private boolean hasValues(List<Integer> values) {
        return values != null && values.isEmpty() == false;
    }

    /**
     * Extracts integer values from a collection while ignoring elements of other types.
     *
     * @param value candidate collection
     * @return integer values, or {@code null} when no integers are available
     */
    private List<Integer> asIntegerList(Object value) {
        if ((value instanceof Collection<?>) == false) return null;

        Collection<?> collection = (Collection<?>) value;
        if (collection.isEmpty()) return null;

        List<Integer> result = new ArrayList<>(collection.size());
        for (Object item : collection) {
            if (item instanceof Integer integerValue) result.add(integerValue);
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * Executes a parameterized chunk search and maps source identity, text, and score.
     *
     * @param dataSourceName resolved RAG datasource name
     * @param sql search query with result columns expected by the mapper
     * @param params ordered query parameters
     * @return mapped search results
     */
    private List<VectorSearchResult> executeSearchQuery(
        String dataSourceName,
        String sql,
        List<Object> params
    ) {
        try (Connection connection = getConnection(dataSourceName);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<VectorSearchResult> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(new VectorSearchResult(
                        resultSet.getLong("id"),
                        resultSet.getString("entity_type"),
                        resultSet.getLong("entity_id"),
                        resultSet.getInt("chunk_index"),
                        resultSet.getString("chunk_text"),
                        resultSet.getDouble("similarity")
                    ));
                }
                return results;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("MariaDB vector search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        if (Constants.getBoolean("ragSemanticSearchEnabled") == false) return false;

        String metric = configuredMetric();
        if (isSupportedMetric(metric) == false) {
            logUnsupportedMetric(metric);
            return false;
        }

        VectorStoreDataSourceResolver.Resolution resolution = VectorStoreDataSourceResolver.resolve();
        return resolution.backend() == VectorStoreType.MARIADB;
    }

    @Override
    public boolean isAvailableAndInitialized() {
        if (isAvailable() == false) return false;

        int dimensions = Constants.getInt("ragEmbeddingDimensions");
        if (validateConfiguration(dimensions) == false) return false;

        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) return false;

        try (Connection connection = getConnection(dataSourceName)) {
            int tableCount = queryForInt(connection, """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('rag_embedding_chunks', 'rag_embedding_vectors')
                """);
            if (tableCount != 2) return false;
            if (queryForInt(connection, """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'rag_embedding_chunks'
                  AND (column_name IN ('source_title', 'source_hash')
                    OR (column_name = 'source_path' AND LOWER(data_type) = 'text'))
                """) != 3) return false;
            if (hasExpectedVectorColumnDefinition(connection, dimensions) == false) return false;
            if (hasExpectedChunkIndexDefinitions(connection) == false) return false;
            if (hasExpectedVectorForeignKeyDefinition(connection) == false) return false;
            return hasExpectedVectorIndexDefinition(connection, configuredMetric());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks the vector column type, dimensions, and non-null constraint.
     *
     * @param connection open database connection
     * @param dimensions required vector dimension count
     * @return whether the stored column matches the required definition
     * @throws SQLException if schema metadata cannot be queried
     */
    private boolean hasExpectedVectorColumnDefinition(Connection connection, int dimensions) throws SQLException {
        return queryForInt(connection, """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'rag_embedding_vectors'
              AND column_name = 'embedding'
              AND LOWER(column_type) = ?
              AND UPPER(data_type) = 'VECTOR'
              AND UPPER(is_nullable) = 'NO'
            """, "vector(" + dimensions + ")") == 1;
    }

    /**
     * Checks whether the vector table already has an embedding column.
     *
     * @param connection open database connection
     * @return whether the embedding column exists
     * @throws SQLException if schema metadata cannot be queried
     */
    private boolean vectorColumnExists(Connection connection) throws SQLException {
        return queryForInt(connection, """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'rag_embedding_vectors'
              AND column_name = 'embedding'
            """) == 1;
    }

    private boolean hasExpectedChunkIndexDefinitions(Connection connection) throws SQLException {
        return matchesChunkIndexDefinitions(showCreateTable(connection, CHUNK_TABLE_NAME));
    }

    /**
     * Checks the required unique, entity, domain/language/status, and full-text chunk indexes.
     *
     * @param createTableSql SHOW CREATE TABLE output, possibly null
     * @return {@code true} when all required chunk index definitions are present
     */
    static boolean matchesChunkIndexDefinitions(String createTableSql) {
        String normalized = normalizeCreateTableSql(createTableSql);
        if (normalized.isEmpty()) return false;

        return matchesChunkUniqueIndexDefinition(normalized) &&
            matchesOrdinaryIndexDefinition(
                normalized, ENTITY_INDEX_NAME, "(entity_type,entity_id)") &&
            matchesOrdinaryIndexDefinition(
                normalized, DOMAIN_LANG_INDEX_NAME, "(domain_id,language,status)") &&
            normalized.contains("fulltext key " + CHUNK_TEXT_FTS_INDEX_NAME + " (chunk_text)");
    }

    private static boolean matchesChunkUniqueIndexDefinition(String normalizedCreateTableSql) {
        String columns = "(entity_type,entity_id,chunk_index,embedding_provider,embedding_model)";
        return normalizedCreateTableSql.contains("unique key " + UNIQUE_CHUNK_INDEX_NAME + " " + columns) ||
            normalizedCreateTableSql.contains("constraint " + UNIQUE_CHUNK_INDEX_NAME + " unique " + columns);
    }

    private static boolean matchesOrdinaryIndexDefinition(
        String normalizedCreateTableSql,
        String indexName,
        String columns
    ) {
        return normalizedCreateTableSql.contains(",key " + indexName + " " + columns);
    }

    private boolean hasExpectedVectorForeignKeyDefinition(Connection connection) throws SQLException {
        return matchesVectorForeignKeyDefinition(showCreateTable(connection, VECTOR_TABLE_NAME));
    }

    /**
     * Checks for cascading deletion from chunk metadata to stored vectors.
     *
     * @param createTableSql SHOW CREATE TABLE output, possibly null
     * @return {@code true} when the chunk foreign key includes ON DELETE CASCADE
     */
    static boolean matchesVectorForeignKeyDefinition(String createTableSql) {
        return normalizeCreateTableSql(createTableSql).contains(
            "foreign key (chunk_id) references rag_embedding_chunks (id) on delete cascade");
    }

    private boolean hasExpectedVectorIndexDefinition(Connection connection, String metric) throws SQLException {
        return matchesVectorIndexDefinition(showCreateTable(connection, VECTOR_TABLE_NAME), metric);
    }

    /**
     * Checks the canonical vector index and its configured distance metric.
     *
     * @param createTableSql SHOW CREATE TABLE output, possibly null
     * @param metric requested metric, with {@code l2} mapped to Euclidean distance
     * @return {@code true} when the canonical embedding index and expected distance are present
     */
    static boolean matchesVectorIndexDefinition(String createTableSql, String metric) {
        if (createTableSql == null) return false;

        String expectedDistance = "l2".equals(metric) ? "euclidean" : "cosine";
        String normalized = normalizeCreateTableSql(createTableSql);
        return normalized.contains("vector key " + VECTOR_INDEX_NAME + " (embedding)") &&
            normalized.contains("distance=" + expectedDistance);
    }

    /**
     * Normalizes quoting, case, and whitespace for comparison of schema definitions.
     *
     * @param createTableSql SHOW CREATE TABLE output, possibly null
     * @return normalized SQL, or an empty string for null input
     */
    private static String normalizeCreateTableSql(String createTableSql) {
        if (createTableSql == null) return "";
        return createTableSql
            .toLowerCase(Locale.ROOT)
            .replace("`", "")
            .replace("'", "")
            .replaceAll("\\s+", " ")
            .replaceAll("\\s*=\\s*", "=")
            .replaceAll("\\(\\s*", "(")
            .replaceAll("\\s*\\)", ")")
            .replaceAll("\\s*,\\s*", ",");
    }

    @Override
    public boolean initializeSchema() {
        int dimensions = Constants.getInt("ragEmbeddingDimensions");
        if (validateConfiguration(dimensions) == false) return false;

        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) {
            Logger.println(MariaDbVectorStore.class,
                "MariaDB RAG datasource not available, skipping schema initialization");
            return false;
        }

        try (Connection connection = getConnection(dataSourceName)) {
            acquireSchemaLock(connection);
            try {
                executeStatement(connection, CREATE_CHUNK_TABLE_SQL);
                migrateChunkTable(connection);
                ensureChunkIndexes(connection);
                executeStatement(connection, CREATE_VECTOR_TABLE_SQL.formatted(dimensions));
                ensureVectorForeignKey(connection);

                if (hasExpectedVectorColumnDefinition(connection, dimensions)) {
                    ensureVectorIndex(connection, configuredMetric());
                } else {
                    resetVectorDimensions(connection, dimensions, configuredMetric());
                }
            } finally {
                releaseSchemaLock(connection);
            }
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Error initializing MariaDB RAG schema: " + e.getMessage());
            return false;
        }
        Logger.println(MariaDbVectorStore.class, "MariaDB RAG vector schema initialized successfully");
        return true;
    }

    /**
     * Adds source and group metadata columns and backfills missing embedding providers.
     *
     * @param connection open database connection
     * @throws SQLException if a schema or provider backfill statement fails
     */
    private void migrateChunkTable(Connection connection) throws SQLException {
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(100)");

        String defaultProvider = Constants.getString("ragEmbeddingProvider");
        if (Tools.isEmpty(defaultProvider)) defaultProvider = "openai";
        executeUpdate(
            connection,
            "UPDATE rag_embedding_chunks SET embedding_provider = ? " +
                "WHERE embedding_provider IS NULL OR embedding_provider = ''",
            defaultProvider.trim().toLowerCase(Locale.ROOT)
        );

        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks MODIFY COLUMN embedding_provider VARCHAR(100) NOT NULL");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS group_id INT");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS root_group_l1 INT");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS root_group_l2 INT");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS root_group_l3 INT");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS source_path TEXT");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks MODIFY COLUMN source_path TEXT NULL");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS source_title VARCHAR(512)");
        executeStatement(connection,
            "ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS source_hash VARCHAR(64)");
    }

    private void ensureChunkIndexes(Connection connection) throws SQLException {
        String createTableSql = showCreateTable(connection, CHUNK_TABLE_NAME);
        Set<String> indexNames = getIndexTypes(connection, CHUNK_TABLE_NAME).keySet();
        String alterSql = buildChunkIndexRepairSql(createTableSql, indexNames);
        if (alterSql != null) executeStatement(connection, alterSql);
    }

    /**
     * Builds repairs for missing or incorrect chunk indexes while preserving unrelated indexes.
     *
     * @param createTableSql current chunk-table definition
     * @param existingIndexNames names of indexes already present
     * @return combined ALTER TABLE statement, or {@code null} when no repair is needed
     */
    static String buildChunkIndexRepairSql(String createTableSql, Collection<String> existingIndexNames) {
        String normalized = normalizeCreateTableSql(createTableSql);
        List<String> clauses = new ArrayList<>();

        appendIndexRepair(
            clauses,
            existingIndexNames,
            UNIQUE_CHUNK_INDEX_NAME,
            matchesChunkUniqueIndexDefinition(normalized),
            "ADD UNIQUE INDEX " + quoteIdentifier(UNIQUE_CHUNK_INDEX_NAME) +
                " (entity_type, entity_id, chunk_index, embedding_provider, embedding_model)"
        );
        appendIndexRepair(
            clauses,
            existingIndexNames,
            ENTITY_INDEX_NAME,
            matchesOrdinaryIndexDefinition(normalized, ENTITY_INDEX_NAME, "(entity_type,entity_id)"),
            "ADD INDEX " + quoteIdentifier(ENTITY_INDEX_NAME) + " (entity_type, entity_id)"
        );
        appendIndexRepair(
            clauses,
            existingIndexNames,
            DOMAIN_LANG_INDEX_NAME,
            matchesOrdinaryIndexDefinition(
                normalized, DOMAIN_LANG_INDEX_NAME, "(domain_id,language,status)"),
            "ADD INDEX " + quoteIdentifier(DOMAIN_LANG_INDEX_NAME) + " (domain_id, language, status)"
        );
        appendIndexRepair(
            clauses,
            existingIndexNames,
            CHUNK_TEXT_FTS_INDEX_NAME,
            normalized.contains("fulltext key " + CHUNK_TEXT_FTS_INDEX_NAME + " (chunk_text)"),
            "ADD FULLTEXT INDEX " + quoteIdentifier(CHUNK_TEXT_FTS_INDEX_NAME) + " (chunk_text)"
        );

        return clauses.isEmpty() ? null : "ALTER TABLE rag_embedding_chunks " + String.join(", ", clauses);
    }

    /**
     * Adds replacement clauses for an incorrect index, retaining the spelling of an existing name when dropping it.
     *
     * @param clauses ALTER TABLE clauses to extend
     * @param existingIndexNames current index names
     * @param expectedName canonical index name to locate case-insensitively
     * @param correct whether the existing definition already matches
     * @param addClause trusted SQL clause creating the required index
     */
    private static void appendIndexRepair(
        List<String> clauses,
        Collection<String> existingIndexNames,
        String expectedName,
        boolean correct,
        String addClause
    ) {
        if (correct) return;

        String existingName = findIdentifier(existingIndexNames, expectedName);
        if (existingName != null) clauses.add("DROP INDEX " + quoteIdentifier(existingName));
        clauses.add(addClause);
    }

    /**
     * Replaces conflicting vector foreign keys with the required cascading chunk reference.
     *
     * @param connection open database connection
     * @throws SQLException if schema inspection or foreign-key replacement fails
     */
    private void ensureVectorForeignKey(Connection connection) throws SQLException {
        String createTableSql = showCreateTable(connection, VECTOR_TABLE_NAME);
        if (matchesVectorForeignKeyDefinition(createTableSql)) return;

        for (String foreignKeyName : getRelevantVectorForeignKeyNames(connection)) {
            executeStatement(connection,
                "ALTER TABLE rag_embedding_vectors DROP FOREIGN KEY " + quoteIdentifier(foreignKeyName));
        }
        executeStatement(connection,
            "ALTER TABLE rag_embedding_vectors ADD CONSTRAINT " + quoteIdentifier(VECTOR_FOREIGN_KEY_NAME) +
                " FOREIGN KEY (chunk_id) REFERENCES rag_embedding_chunks(id) ON DELETE CASCADE");
    }

    @Override
    public boolean recreateIndex() {
        if (validateMetric() == false) return false;

        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) {
            Logger.println(MariaDbVectorStore.class,
                "MariaDB RAG datasource not available, skipping vector index recreation");
            return false;
        }

        String metric = configuredMetric();
        try (Connection connection = getConnection(dataSourceName)) {
            acquireSchemaLock(connection);
            try {
                ensureVectorIndex(connection, metric);
            } finally {
                releaseSchemaLock(connection);
            }
            Logger.println(MariaDbVectorStore.class,
                "MariaDB RAG vector index is ready with distance metric: " + metric);
            return true;
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Error recreating MariaDB RAG vector index: " + e.getMessage());
            return false;
        }
    }

    /**
     * Keeps a matching canonical vector index or replaces conflicting vector indexes.
     *
     * @param connection open database connection
     * @param metric supported distance metric
     * @throws SQLException if index inspection or replacement fails
     */
    private void ensureVectorIndex(Connection connection, String metric) throws SQLException {
        Map<String, String> indexTypes = getIndexTypes(connection, VECTOR_TABLE_NAME);
        List<String> vectorIndexNames = indexNamesOfType(indexTypes, "VECTOR");
        if (vectorIndexNames.size() == 1 &&
            VECTOR_INDEX_NAME.equalsIgnoreCase(vectorIndexNames.get(0)) &&
            hasExpectedVectorIndexDefinition(connection, metric)) {
            return;
        }

        Set<String> indexesToDrop = new LinkedHashSet<>(vectorIndexNames);
        String canonicalIndex = findIdentifier(indexTypes.keySet(), VECTOR_INDEX_NAME);
        if (canonicalIndex != null) indexesToDrop.add(canonicalIndex);
        executeStatement(connection, buildVectorIndexRepairSql(indexesToDrop, metric));
    }

    /**
     * Builds a single ALTER TABLE statement to replace selected indexes with the canonical vector index.
     *
     * @param indexesToDrop existing index names to remove
     * @param metric supported distance metric
     * @return SQL that drops the selected indexes and adds the configured vector index
     */
    static String buildVectorIndexRepairSql(Collection<String> indexesToDrop, String metric) {
        List<String> clauses = new ArrayList<>();
        for (String indexName : indexesToDrop) {
            clauses.add("DROP INDEX " + quoteIdentifier(indexName));
        }
        clauses.add(vectorIndexAddClause(metric));
        return "ALTER TABLE rag_embedding_vectors " + String.join(", ", clauses);
    }

    @Override
    public boolean resetDimensions(int dimensions) {
        if (validateConfiguration(dimensions) == false) return false;

        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) {
            Logger.println(MariaDbVectorStore.class,
                "MariaDB RAG datasource not available, skipping dimension reset");
            return false;
        }

        try (Connection connection = getConnection(dataSourceName)) {
            acquireSchemaLock(connection);
            try {
                ensureVectorForeignKey(connection);
                resetVectorDimensions(connection, dimensions, configuredMetric());
            } finally {
                releaseSchemaLock(connection);
            }
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Error resetting MariaDB RAG vector dimensions: " + e.getMessage());
            return false;
        }
        Logger.println(MariaDbVectorStore.class,
            "MariaDB RAG embedding data deleted and vector dimensions updated to " + dimensions);
        return true;
    }

    /**
     * Deletes all chunk metadata and cascaded vectors before resizing the vector column and rebuilding its index.
     *
     * @param connection open database connection
     * @param dimensions validated new vector dimension count
     * @param metric supported distance metric
     * @throws SQLException if inspection, data deletion, or schema alteration fails
     */
    private void resetVectorDimensions(Connection connection, int dimensions, String metric) throws SQLException {
        boolean columnExists = vectorColumnExists(connection);
        Map<String, String> indexTypes = getIndexTypes(connection, VECTOR_TABLE_NAME);
        Set<String> indexesToDrop = new LinkedHashSet<>(indexNamesOfType(indexTypes, "VECTOR"));
        String canonicalIndex = findIdentifier(indexTypes.keySet(), VECTOR_INDEX_NAME);
        if (canonicalIndex != null) indexesToDrop.add(canonicalIndex);

        executeStatement(connection, "DELETE FROM rag_embedding_chunks");
        executeStatement(connection,
            buildVectorDimensionResetSql(indexesToDrop, dimensions, metric, columnExists));
    }

    /**
     * Builds the vector-column resize and index recreation statement for an emptied vector table.
     *
     * @param indexesToDrop existing index names to remove
     * @param dimensions validated new vector dimension count
     * @param metric supported distance metric
     * @param columnExists whether the embedding column must be modified instead of added
     * @return ALTER TABLE statement for the column and canonical vector index
     */
    static String buildVectorDimensionResetSql(
        Collection<String> indexesToDrop,
        int dimensions,
        String metric,
        boolean columnExists
    ) {
        List<String> clauses = new ArrayList<>();
        for (String indexName : indexesToDrop) {
            clauses.add("DROP INDEX " + quoteIdentifier(indexName));
        }
        clauses.add((columnExists ? "MODIFY COLUMN" : "ADD COLUMN") +
            " embedding VECTOR(" + dimensions + ") NOT NULL");
        clauses.add(vectorIndexAddClause(metric));
        return "ALTER TABLE rag_embedding_vectors " + String.join(", ", clauses);
    }

    private static String vectorIndexAddClause(String metric) {
        String indexDistance = "l2".equals(metric) ? "euclidean" : "cosine";
        return "ADD VECTOR INDEX " + quoteIdentifier(VECTOR_INDEX_NAME) +
            " (embedding) M=16 DISTANCE=" + indexDistance;
    }

    @Override
    public Map<String, float[]> getExistingEmbeddingsByHash(
        String entityType,
        long entityId,
        String embeddingProvider,
        String embeddingModel,
        int domainId
    ) {
        String dataSourceName = getMariaDataSourceName();
        if (dataSourceName == null) return new HashMap<>();

        try (Connection connection = getConnection(dataSourceName);
             PreparedStatement statement = connection.prepareStatement(GET_EXISTING_HASH_EMBEDDING_SQL)) {
            bindParameters(
                statement,
                List.of(entityType, entityId, embeddingProvider, embeddingModel, domainId)
            );
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, float[]> result = new HashMap<>();
                while (resultSet.next()) {
                    result.put(
                        resultSet.getString("content_hash"),
                        parseVector(resultSet.getString("embedding_text"))
                    );
                }
                return result;
            }
        } catch (Exception e) {
            Logger.error(MariaDbVectorStore.class,
                "Error fetching existing MariaDB embeddings for " + entityType + "/" + entityId +
                ": " + e.getMessage());
            return new HashMap<>();
        }
    }

    private String getMariaDataSourceName() {
        VectorStoreDataSourceResolver.Resolution resolution = VectorStoreDataSourceResolver.resolve();
        if (resolution.backend() != VectorStoreType.MARIADB) return null;
        return resolution.dataSourceName();
    }

    /**
     * Validates the MariaDB vector dimension range and configured distance metric, logging invalid settings.
     *
     * @param dimensions requested vector dimension count
     * @return {@code true} for 1 through 16383 dimensions and a supported metric
     */
    private boolean validateConfiguration(int dimensions) {
        if (dimensions < 1 || dimensions > MAX_VECTOR_DIMENSIONS) {
            Logger.error(MariaDbVectorStore.class,
                "Invalid MariaDB RAG embedding dimensions: " + dimensions +
                "; expected a value from 1 to " + MAX_VECTOR_DIMENSIONS);
            return false;
        }

        return validateMetric();
    }

    private boolean validateMetric() {
        String metric = configuredMetric();
        if (isSupportedMetric(metric)) return true;

        logUnsupportedMetric(metric);
        return false;
    }

    static boolean isSupportedMetric(String metric) {
        return "cosine".equals(metric) || "l2".equals(metric);
    }

    static String distanceFunction(String metric) {
        return "l2".equals(metric) ? "VEC_DISTANCE_EUCLIDEAN" : "VEC_DISTANCE_COSINE";
    }

    private String configuredMetric() {
        String metric = Constants.getString("ragSearchDistanceMetric");
        return metric == null ? "" : metric.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Logs a configuration error explaining the metrics supported by MariaDB Vector.
     *
     * @param metric unsupported configured metric
     */
    private void logUnsupportedMetric(String metric) {
        if ("inner_product".equals(metric)) {
            Logger.error(MariaDbVectorStore.class,
                "MariaDB does not support the configured inner_product vector distance metric; " +
                "use cosine or l2");
        } else {
            Logger.error(MariaDbVectorStore.class,
                "Unsupported MariaDB vector distance metric '" + metric + "'; use cosine or l2");
        }
    }

    /**
     * Serializes vector components in the bracketed format accepted by VEC_FromText.
     *
     * @param embedding vector components in dimension order
     * @return comma-separated vector enclosed in brackets
     */
    static String vectorToString(float[] embedding) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) result.append(",");
            result.append(embedding[i]);
        }
        return result.append("]").toString();
    }

    /**
     * Parses the bracketed representation returned by VEC_ToText.
     *
     * @param vector vector text, possibly null or empty
     * @return parsed components, or an empty array for missing or empty bracket contents
     */
    static float[] parseVector(String vector) {
        if (vector == null || vector.length() < 2) return new float[0];

        String inner = vector.substring(1, vector.length() - 1).trim();
        if (inner.isEmpty()) return new float[0];

        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private String showCreateTable(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW CREATE TABLE " + tableName)) {
            if (resultSet.next()) return resultSet.getString(2);
        }
        throw new SQLException("Unable to read MariaDB table definition for " + tableName);
    }

    /**
     * Reads index names and types for a table in the current database.
     *
     * @param connection open database connection
     * @param tableName table whose index metadata is requested
     * @return index types keyed by index name in name order
     * @throws SQLException if index metadata cannot be read
     */
    private Map<String, String> getIndexTypes(Connection connection, String tableName) throws SQLException {
        Map<String, String> result = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT DISTINCT index_name, index_type
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = ?
            ORDER BY index_name
            """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(resultSet.getString("index_name"), resultSet.getString("index_type"));
                }
            }
        }
        return result;
    }

    /**
     * Finds foreign keys on the vector chunk ID or using the canonical constraint name.
     *
     * @param connection open database connection
     * @return matching foreign-key names in name order
     * @throws SQLException if constraint metadata cannot be read
     */
    private List<String> getRelevantVectorForeignKeyNames(Connection connection) throws SQLException {
        List<String> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT DISTINCT constraint_name
            FROM information_schema.key_column_usage
            WHERE table_schema = DATABASE()
              AND table_name = 'rag_embedding_vectors'
              AND referenced_table_name IS NOT NULL
              AND (column_name = 'chunk_id' OR constraint_name = ?)
            ORDER BY constraint_name
            """)) {
            statement.setString(1, VECTOR_FOREIGN_KEY_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) result.add(resultSet.getString("constraint_name"));
            }
        }
        return result;
    }

    private static List<String> indexNamesOfType(Map<String, String> indexTypes, String expectedType) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : indexTypes.entrySet()) {
            if (expectedType.equalsIgnoreCase(entry.getValue())) result.add(entry.getKey());
        }
        return result;
    }

    private static String findIdentifier(Collection<String> identifiers, String expectedIdentifier) {
        for (String identifier : identifiers) {
            if (expectedIdentifier.equalsIgnoreCase(identifier)) return identifier;
        }
        return null;
    }

    private static String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * Acquires the connection-scoped RAG schema lock with a 30-second timeout.
     *
     * @param connection open database connection
     * @throws SQLException if the lock cannot be acquired or the lock query fails
     */
    private void acquireSchemaLock(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, SCHEMA_LOCK_NAME);
            statement.setInt(2, SCHEMA_LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() == false || resultSet.getInt(1) != 1) {
                    throw new SQLException("Timed out waiting for the MariaDB RAG schema lock");
                }
            }
        }
    }

    private void releaseSchemaLock(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, SCHEMA_LOCK_NAME);
            statement.executeQuery().close();
        } catch (SQLException e) {
            Logger.error(MariaDbVectorStore.class,
                "Failed to release the MariaDB RAG schema lock: " + e.getMessage());
        }
    }

    /**
     * Obtains a pooled connection and rejects a missing connection explicitly.
     *
     * @param dataSourceName resolved RAG datasource name
     * @return open connection that the caller must close
     * @throws SQLException if a connection cannot be obtained
     */
    protected Connection getConnection(String dataSourceName) throws SQLException {
        Connection connection = DBPool.getConnection(dataSourceName);
        if (connection == null) {
            throw new SQLException("Unable to obtain database connection for " + dataSourceName);
        }
        return connection;
    }

    private void executeStatement(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private int executeUpdate(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        }
    }

    /**
     * Executes a parameterized query and reads the first column of its first row as an integer.
     *
     * @param connection open database connection
     * @param sql query text
     * @param params ordered query parameters
     * @return first integer value, or zero if no row is returned
     * @throws SQLException if query execution or result access fails
     */
    private int queryForInt(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    private void bindParameters(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private void rollback(Connection connection, Exception originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    private void restoreAutoCommit(Connection connection, boolean autoCommit) {
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            Logger.error(MariaDbVectorStore.class,
                "Failed to restore MariaDB connection auto-commit state: " + e.getMessage());
        }
    }
}
