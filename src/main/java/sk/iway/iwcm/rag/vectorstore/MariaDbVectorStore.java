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
import sk.iway.iwcm.common.CloudToolsForCore;
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

    private void addEntityTypeSpecificConditions(
        StringBuilder sql,
        List<Object> params,
        String columnPrefix,
        RagEntityType entityType,
        Map<String, Object> bonusParams
    ) {
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
        return resolution.backend() == VectorStoreBackend.MARIADB;
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
            if (hasExpectedVectorColumnDefinition(connection, dimensions) == false) return false;
            if (hasExpectedChunkIndexDefinitions(connection) == false) return false;
            if (hasExpectedVectorForeignKeyDefinition(connection) == false) return false;
            return hasExpectedVectorIndexDefinition(connection, configuredMetric());
        } catch (Exception e) {
            return false;
        }
    }

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

    static boolean matchesVectorForeignKeyDefinition(String createTableSql) {
        return normalizeCreateTableSql(createTableSql).contains(
            "foreign key (chunk_id) references rag_embedding_chunks (id) on delete cascade");
    }

    private boolean hasExpectedVectorIndexDefinition(Connection connection, String metric) throws SQLException {
        return matchesVectorIndexDefinition(showCreateTable(connection, VECTOR_TABLE_NAME), metric);
    }

    static boolean matchesVectorIndexDefinition(String createTableSql, String metric) {
        if (createTableSql == null) return false;

        String expectedDistance = "l2".equals(metric) ? "euclidean" : "cosine";
        String normalized = normalizeCreateTableSql(createTableSql);
        return normalized.contains("vector key " + VECTOR_INDEX_NAME + " (embedding)") &&
            normalized.contains("distance=" + expectedDistance);
    }

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
    }

    private void ensureChunkIndexes(Connection connection) throws SQLException {
        String createTableSql = showCreateTable(connection, CHUNK_TABLE_NAME);
        Set<String> indexNames = getIndexTypes(connection, CHUNK_TABLE_NAME).keySet();
        String alterSql = buildChunkIndexRepairSql(createTableSql, indexNames);
        if (alterSql != null) executeStatement(connection, alterSql);
    }

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

    /** {@inheritDoc} */
    @Override
    @Deprecated(forRemoval = false)
    public Map<String, float[]> getExistingEmbeddingsByHash(
        String entityType,
        long entityId,
        String embeddingProvider,
        String embeddingModel
    ) {
        return getExistingEmbeddingsByHash(
            entityType,
            entityId,
            embeddingProvider,
            embeddingModel,
            CloudToolsForCore.getDomainId()
        );
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
        if (resolution.backend() != VectorStoreBackend.MARIADB) return null;
        return resolution.dataSourceName();
    }

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

    static String vectorToString(float[] embedding) {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) result.append(",");
            result.append(embedding[i]);
        }
        return result.append("]").toString();
    }

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
