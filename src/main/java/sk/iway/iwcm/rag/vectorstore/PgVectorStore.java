package sk.iway.iwcm.rag.vectorstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.rag.vectorjpa.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/**
 * PgVector implementation of VectorStore.
 * Handles ONLY the embedding (vector) column via native SQL using pgvector operators.
 * Entity CRUD operations are handled by EmbeddingChunkRepository (JPA).
 * Connects to the RAG datasource (primary PgSQL or secondary rag_jpa).
 */
@Service
public class PgVectorStore implements VectorStore {

    private static final String DIMENSION_PLACEHOLDER = "{DIMENSION_PLACEHOLDER}";

    private String getDataSourceName() {
        VectorStoreDataSourceResolver.Resolution resolution = VectorStoreDataSourceResolver.resolve();
        if (resolution.backend() != VectorStoreType.POSTGRESQL) return null;
        return resolution.dataSourceName();
    }

    private static final String CREATE_EXTENSION_SQL = "CREATE EXTENSION IF NOT EXISTS vector";

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS rag_embedding_chunks (
            id              BIGSERIAL PRIMARY KEY,
            entity_type     VARCHAR(100) NOT NULL,
            entity_id       BIGINT NOT NULL,
            chunk_index     INT NOT NULL,
            chunk_text      TEXT NOT NULL,
            content_hash    VARCHAR(64) NOT NULL,
            source_path     TEXT,
            source_title    VARCHAR(512),
            source_hash     VARCHAR(64),
            embedding       vector(%s),
            embedding_provider VARCHAR(100) NOT NULL,
            embedding_model VARCHAR(100) NOT NULL,
            dimensions      INT NOT NULL,
            language        VARCHAR(10),
            domain_id       INT,
            group_id        INT,
            root_group_l1   INT,
            root_group_l2   INT,
            root_group_l3   INT,
            status          VARCHAR(20) NOT NULL DEFAULT '%s',
            error_message   VARCHAR(500),
            create_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT uq_rag_chunk UNIQUE (entity_type, entity_id, chunk_index, embedding_provider, embedding_model)
        )
        """.formatted(DIMENSION_PLACEHOLDER, EmbeddingChunkStatus.COMPLETED.name());

    private static final String CREATE_ENTITY_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_entity ON rag_embedding_chunks (entity_type, entity_id)";

    private static final String CREATE_DOMAIN_LANG_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_domain_lang ON rag_embedding_chunks (domain_id, language, status)";

    private static final String CREATE_CHUNK_TEXT_FTS_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_text_fts ON rag_embedding_chunks USING GIN (to_tsvector('simple', chunk_text))";

    private static final String DROP_HNSW_INDEX_SQL =
        "DROP INDEX IF EXISTS idx_rag_embedding_hnsw";

    private static final String UPDATE_EMBEDDING_SQL =
        "UPDATE rag_embedding_chunks SET embedding = ?::vector, status = '" +
        EmbeddingChunkStatus.COMPLETED.name() + "', error_message = NULL WHERE id = ?";

    private static final String MARK_EMBEDDING_ERROR_SQL =
        "UPDATE rag_embedding_chunks SET status = '" + EmbeddingChunkStatus.ERROR.name() +
        "', error_message = ? WHERE id = ?";

    /**
     * Builds the vector-search query prefix for the configured distance metric.
     * Supports cosine distance, negated inner product, and normalized L2 distance.
     *
     * @return SQL prefix requiring vector, provider, and model parameters
     */
    private static String buildSearchSql() {
        String metric = Constants.getString("ragSearchDistanceMetric");
        String similarityCalc = switch (metric) {
            case "inner_product" -> "(embedding <#> ?::vector) * -1 AS similarity";
            case "l2" -> "1 / (1 + (embedding <-> ?::vector)) AS similarity";
            default -> "1 - (embedding <=> ?::vector) AS similarity";
        };
        return "SELECT id, entity_type, entity_id, chunk_index, chunk_text," +
               "       " + similarityCalc +
               " FROM rag_embedding_chunks" +
               " WHERE status = '" + EmbeddingChunkStatus.COMPLETED.name() + "'" +
               " AND embedding IS NOT NULL" +
               " AND embedding_provider = ?" +
               " AND embedding_model = ?";
    }

    private static final String GET_EXISTING_HASH_EMBEDDING =
        "SELECT content_hash, embedding::text AS embedding_text FROM rag_embedding_chunks " +
        "WHERE entity_type = ? AND entity_id = ? AND embedding_provider = ? AND embedding_model = ? AND domain_id = ? AND status = '" + EmbeddingChunkStatus.COMPLETED.name() + "'";

    private static final String DELETE_ALL_DATA_SQL =
        "DELETE FROM rag_embedding_chunks";

    private static final String FTS_SEARCH_SQL_PREFIX =
        "SELECT id, entity_type, entity_id, chunk_index, chunk_text, " +
        "ts_rank_cd(to_tsvector('simple', chunk_text), websearch_to_tsquery('simple', ?)) AS similarity " +
        "FROM rag_embedding_chunks " +
        "WHERE status = '" + EmbeddingChunkStatus.COMPLETED.name() + "' " +
        "AND embedding_provider = ? " +
        "AND embedding_model = ? " +
        "AND to_tsvector('simple', chunk_text) @@ websearch_to_tsquery('simple', ?)";

    private static final String FTS_ILIKE_SQL_PREFIX =
        "SELECT id, entity_type, entity_id, chunk_index, chunk_text, " +
        "CASE WHEN position(lower(?) in lower(chunk_text)) > 0 THEN 1.0 ELSE 0.0 END AS similarity " +
        "FROM rag_embedding_chunks " +
        "WHERE status = '" + EmbeddingChunkStatus.COMPLETED.name() + "' " +
        "AND embedding_provider = ? " +
        "AND embedding_model = ? " +
        "AND chunk_text ILIKE ?";

    @Override
    public void updateEmbedding(Long id, float[] embedding) {
        if (id == null || embedding == null || embedding.length == 0) return;

        String dsName = getDataSourceName();
        if (dsName == null) return;

        updateEmbeddingRow(dsName, id, embedding);
    }

    /**
     * Stores one vector and marks its chunk completed, recording an error status when the update fails.
     *
     * @param dsName resolved RAG datasource name
     * @param id existing chunk ID
     * @param embedding nonempty embedding vector
     * @return {@code true} when the vector and completed status were stored successfully
     */
    private boolean updateEmbeddingRow(String dsName, Long id, float[] embedding) {
        try {
            int updatedRows = executeUpdate(
                dsName,
                UPDATE_EMBEDDING_SQL,
                vectorToString(embedding),
                id
            );
            if (updatedRows != 1) {
                throw new IllegalStateException("Expected to update one embedding row, updated " + updatedRows);
            }
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class,
                "Error updating embedding for chunk id " + id + ": " + e.getMessage());
            markEmbeddingError(dsName, id, e.getMessage());
            return false;
        }
    }

    @Override
    public void updateEmbeddingBatch(List<Long> ids, List<float[]> embeddings) {
        if (ids == null || ids.isEmpty() || embeddings == null || embeddings.isEmpty()) return;
        if (ids.size() != embeddings.size()) {
            throw new IllegalArgumentException("IDs/embeddings count mismatch: ids=" + ids.size() + ", embeddings=" + embeddings.size());
        }

        String dsName = getDataSourceName();
        if (dsName == null) return;

        try {
            String sql = buildBatchUpdateEmbeddingSql(ids.size());
            List<Object> params = new ArrayList<>(ids.size() * 2);

            for (int i = 0; i < ids.size(); i++) {
                params.add(ids.get(i));
                params.add(vectorToString(embeddings.get(i)));
            }

            // Add all IDs again for the WHERE IN clause
            for (Long id : ids) {
                params.add(id);
            }

            int updatedRows = executeUpdate(dsName, sql, params.toArray());
            if (updatedRows != ids.size()) {
                throw new IllegalStateException(
                    "Expected to update " + ids.size() + " embedding rows, updated " + updatedRows
                );
            }
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error batch updating embeddings, falling back to row-by-row updates: " + e.getMessage());
            int failedRows = 0;
            for (int i = 0; i < ids.size(); i++) {
                if (updateEmbeddingRow(dsName, ids.get(i), embeddings.get(i)) == false) {
                    failedRows++;
                }
            }
            if (failedRows > 0) {
                throw new IllegalStateException(
                    "Failed to update " + failedRows + " of " + ids.size() + " embedding rows",
                    e
                );
            }
        }
    }

    /**
     * Builds a CASE-based batch update that stores vectors and clears chunk error statuses.
     *
     * @param rowCount number of chunk-vector pairs to update
     * @return SQL requiring ID/vector pairs followed by all IDs for the final selection
     */
    private String buildBatchUpdateEmbeddingSql(int rowCount) {
        StringBuilder sql = new StringBuilder("UPDATE rag_embedding_chunks SET embedding = CASE id ");

        for (int i = 0; i < rowCount; i++) {
            sql.append("WHEN ? THEN ?::vector ");
        }

        sql.append("END, status = '").append(EmbeddingChunkStatus.COMPLETED.name());
        sql.append("', error_message = NULL WHERE id IN (");
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        return sql.toString();
    }

    private int executeUpdate(String dsName, String sql, Object... params) throws SQLException {
        try (Connection connection = DBPool.getConnection(dsName);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            SimpleQuery.bindParameters(statement, params);
            return statement.executeUpdate();
        }
    }

    /**
     * Marks a failed vector update with a bounded error message and logs status-update failures.
     *
     * @param dsName resolved RAG datasource name
     * @param id chunk ID to mark as failed
     * @param errorMessage failure message, or empty for the default message
     */
    private void markEmbeddingError(String dsName, Long id, String errorMessage) {
        String message = Tools.isEmpty(errorMessage) ? "Embedding vector update failed" : errorMessage;
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }

        try {
            int updatedRows = executeUpdate(dsName, MARK_EMBEDDING_ERROR_SQL, message, id);
            if (updatedRows != 1) {
                Logger.error(PgVectorStore.class,
                    "Failed to mark embedding chunk " + id + " as ERROR, updated rows: " + updatedRows);
            }
        } catch (Exception statusException) {
            Logger.error(PgVectorStore.class,
                "Failed to mark embedding chunk " + id + " as ERROR: " + statusException.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, String embeddingProvider, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        String dsName = getDataSourceName();
        if (dsName == null) return new ArrayList<>();

        // Apply configured ef_search parameter if not default
        int efSearch = Constants.getInt("ragSearchEfSearch");
        if (efSearch != 40) {
            try {
                new SimpleQuery(dsName).execute("SET LOCAL hnsw.ef_search = ?", efSearch);
            } catch (Exception e) {
                Logger.error(PgVectorStore.class, "Failed to set hnsw.ef_search to " + efSearch + ": " + e.getMessage());
            }
        }

        String metric = Constants.getString("ragSearchDistanceMetric");
        String orderOperator = switch (metric) {
            case "inner_product" -> "<#>";
            case "l2" -> "<->";
            default -> "<=>";
        };

        StringBuilder sql = new StringBuilder(buildSearchSql());
        List<Object> params = new ArrayList<>();
        params.add(vectorToString(queryEmbedding));
        params.add(embeddingProvider);
        params.add(embeddingModel);

        addScopeFilters(sql, params, entityType, domainId, language);

        addEntityTypeSpecificConditions(sql, params, entityType, bonusParams);

        sql.append(" ORDER BY embedding ").append(orderOperator).append(" ?::vector LIMIT ?");
        params.add(vectorToString(queryEmbedding));
        params.add(limit);

        return new ComplexQuery()
            .setSql(sql.toString())
            .setParams(params.toArray())
            .setDatabase(dsName)
            .list(rs -> new VectorSearchResult(
                rs.getLong("id"),
                rs.getString("entity_type"),
                rs.getLong("entity_id"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                rs.getDouble("similarity")
            ));
    }

    @Override
    public List<VectorSearchResult> searchFulltext(String query, String embeddingProvider, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        String dsName = getDataSourceName();
        if (dsName == null || Tools.isEmpty(query) || limit <= 0) return new ArrayList<>();

        List<VectorSearchResult> ftsResults = executeFulltextSearch(dsName, query, embeddingProvider, embeddingModel, entityType, domainId, language, limit, bonusParams);

        boolean hybridFtsUseIlikeFallback = Constants.getBoolean("ragHybridFtsUseIlikeFallback");
        if(bonusParams != null) {
            hybridFtsUseIlikeFallback = Tools.getBooleanValue(
                                            String.valueOf(bonusParams.get("hybridFtsUseIlikeFallback")),
                                            hybridFtsUseIlikeFallback
                                        );
        }

        if (ftsResults.isEmpty() && hybridFtsUseIlikeFallback) {
            return executeIlikeSearch(dsName, query, embeddingProvider, embeddingModel, entityType, domainId, language, limit, bonusParams);
        }

        return ftsResults;
    }

    /**
     * Retrieves completed chunks ranked by native full-text relevance within the requested scope.
     *
     * @param dsName resolved RAG datasource name
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
    private List<VectorSearchResult> executeFulltextSearch(String dsName, String query, String embeddingProvider, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        StringBuilder sql = new StringBuilder(FTS_SEARCH_SQL_PREFIX);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(embeddingProvider);
        params.add(embeddingModel);
        params.add(query);

        addScopeFilters(sql, params, entityType, domainId, language);

        addEntityTypeSpecificConditions(sql, params, entityType, bonusParams);

        sql.append(" ORDER BY similarity DESC LIMIT ?");
        params.add(limit);

        return executeSearchQuery(dsName, sql.toString(), params);
    }

    /**
     * Retrieves completed chunks using a case-insensitive pattern fallback within the requested scope.
     *
     * @param dsName resolved RAG datasource name
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
    private List<VectorSearchResult> executeIlikeSearch(String dsName, String query, String embeddingProvider, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        StringBuilder sql = new StringBuilder(FTS_ILIKE_SQL_PREFIX);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(embeddingProvider);
        params.add(embeddingModel);
        params.add("%" + query + "%");

        addScopeFilters(sql, params, entityType, domainId, language);

        addEntityTypeSpecificConditions(sql, params, entityType, bonusParams);

        sql.append(" ORDER BY similarity DESC, id ASC LIMIT ?");
        params.add(limit);

        return executeSearchQuery(dsName, sql.toString(), params);
    }

    /**
     * Appends optional entity, domain, and language conditions and their bound values.
     *
     * @param sql query builder to extend
     * @param params ordered parameters to extend
     * @param entityType source type, or {@code null} for all types
     * @param domainId exact storage domain, including zero, or {@code null} for all domains
     * @param language language code, or null or empty to omit the filter
     */
    private void addScopeFilters(StringBuilder sql, List<Object> params, RagEntityType entityType, Integer domainId, String language) {
        if (entityType != null) {
            sql.append(" AND entity_type = ?");
            params.add(entityType.name());
        }
        if (domainId != null) {
            sql.append(" AND domain_id = ?");
            params.add(domainId);
        }
        if (Tools.isNotEmpty(language)) {
            sql.append(" AND language = ?");
            params.add(language);
        }
    }

    /**
     * Appends a Markdown root-prefix filter or document-group constraints before result limiting.
     * Markdown root wildcards are escaped; document group conditions are combined with OR.
     *
     * @param sql query builder to extend
     * @param params ordered parameters to extend
     * @param entityType source type that selects the applicable filters
     * @param bonusParams optional {@code sourceRoot}, {@code sourceRoots}, or document root-group collections
     */
    private void addEntityTypeSpecificConditions(StringBuilder sql, List<Object> params, RagEntityType entityType, Map<String, Object> bonusParams) {

        if (bonusParams != null && RagEntityType.MARKDOWN == entityType) {
            List<?> roots = bonusParams.get("sourceRoot") instanceof String sourceRoot ? List.of(sourceRoot) :
                bonusParams.get("sourceRoots") instanceof List<?> sourceRoots ? sourceRoots : null;
            if (roots != null) {
                sql.append(" AND (1=0");
                for (Object value : roots) {
                    if (value instanceof String root) {
                        sql.append(" OR ");
                        sql.append("source_path LIKE ? ESCAPE '!'");
                        params.add(root.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "/%");
                    }
                }
                sql.append(")");
            }
        }

        // Document searches can be narrowed to the search app's selected root groups.
        if(bonusParams != null && RagEntityType.DOCUMENT == entityType) {
            List<Integer> rootGroupsL1 = asIntegerList(bonusParams.get("rootGroupL1"));
            List<Integer> rootGroupsL2 = asIntegerList(bonusParams.get("rootGroupL2"));
            List<Integer> rootGroupsL3 = asIntegerList(bonusParams.get("rootGroupL3"));
            List<Integer> rootGroups = asIntegerList(bonusParams.get("rootGroups"));

            boolean hasL1 = rootGroupsL1 != null && !rootGroupsL1.isEmpty();
            boolean hasL2 = rootGroupsL2 != null && !rootGroupsL2.isEmpty();
            boolean hasL3 = rootGroupsL3 != null && !rootGroupsL3.isEmpty();
            boolean hasRootGroups = rootGroups != null && !rootGroups.isEmpty();

            if(hasL1 || hasL2 || hasL3 || hasRootGroups) {
                sql.append(" AND (");
                boolean first = true;
                if(hasL1) {
                    sql.append("root_group_l1 IN (").append(Tools.join(rootGroupsL1, ",")).append(")");
                    first = false;
                }
                if(hasL2) {
                    if(!first) sql.append(" OR ");
                    sql.append("root_group_l2 IN (").append(Tools.join(rootGroupsL2, ",")).append(")");
                    first = false;
                }
                if(hasL3) {
                    if(!first) sql.append(" OR ");
                    sql.append("root_group_l3 IN (").append(Tools.join(rootGroupsL3, ",")).append(")");
                    first = false;
                }
                if(hasRootGroups) {
                    if(!first) sql.append(" OR ");
                    sql.append("group_id IN (").append(Tools.join(rootGroups, ",")).append(")");
                }
                sql.append(") ");
            }
        }
    }

    /**
     * Extracts integer values from a collection while ignoring elements of other types.
     *
     * @param value candidate collection
     * @return integer values, or {@code null} when the input is not a collection or is empty
     */
    private List<Integer> asIntegerList(Object value) {
        if (!(value instanceof Collection<?>)) return null;

        Collection<?> collection = (Collection<?>) value;
        if (collection.isEmpty()) return null;

        List<Integer> result = new ArrayList<>(collection.size());
        for (Object item : collection) {
            if (item instanceof Integer integerValue) {
                result.add(integerValue);
            }
        }

        return result;
    }

    /**
     * Executes a parameterized chunk search and maps source identity, text, and score.
     *
     * @param dsName resolved RAG datasource name
     * @param sql search query with result columns expected by the mapper
     * @param params ordered query parameters
     * @return mapped search results
     */
    private List<VectorSearchResult> executeSearchQuery(String dsName, String sql, List<Object> params) {
        return new ComplexQuery()
            .setSql(sql)
            .setParams(params.toArray())
            .setDatabase(dsName)
            .list(rs -> new VectorSearchResult(
                rs.getLong("id"),
                rs.getString("entity_type"),
                rs.getLong("entity_id"),
                rs.getInt("chunk_index"),
                rs.getString("chunk_text"),
                rs.getDouble("similarity")
            ));
    }

    @Override
    public boolean isAvailable() {
        return Constants.getBoolean("ragSemanticSearchEnabled") && getDataSourceName() != null;
    }

    @Override
    public boolean isAvailableAndInitialized() {
        // Availability checks the datasource; this query verifies that the schema has been initialized.
        if (isAvailable() == false) return false;

        try {
            SimpleQuery sq = new SimpleQuery(getDataSourceName());
            sq.forInt("SELECT 1, source_path, source_title, source_hash FROM rag_embedding_chunks WHERE embedding_provider IS NOT NULL LIMIT 1");
            return sq.forInt("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = current_schema() AND table_name = 'rag_embedding_chunks'
                  AND column_name = 'source_path' AND LOWER(data_type) = 'text'
                """) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean initializeSchema() {
        String dsName = getDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping schema initialization");
            return false;
        }

        // Shared schema settings must not depend on the domain that triggers initialization.
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(null)) {
            SimpleQuery sq = new SimpleQuery(dsName);
            sq.execute(CREATE_EXTENSION_SQL);
            // Important note: the table creation must be done with the correct dimension count, otherwise the embedding insertions will fail with "vector has wrong number of dimensions" error
            sq.execute( Tools.replace(CREATE_TABLE_SQL, DIMENSION_PLACEHOLDER, Constants.getInt("ragEmbeddingDimensions") + "") );

            migrateEmbeddingProviderColumn(sq);

            if (recreateHnswIndex() == false) {
                return false;
            }

            sq.execute(CREATE_ENTITY_INDEX_SQL);
            sq.execute(CREATE_DOMAIN_LANG_INDEX_SQL);
            sq.execute(CREATE_CHUNK_TEXT_FTS_INDEX_SQL);

            // Migrate existing tables: add group columns if they do not exist yet
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS group_id INT");
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS root_group_l1 INT");
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS root_group_l2 INT");
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS root_group_l3 INT");
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS source_path TEXT");
            sq.execute("ALTER TABLE rag_embedding_chunks ALTER COLUMN source_path TYPE TEXT");
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS source_title VARCHAR(512)");
            sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS source_hash VARCHAR(64)");

            Logger.println(PgVectorStore.class, "RAG pgvector schema initialized successfully");
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error initializing RAG pgvector schema: " + e.getMessage());
            return false;
        }
    }

    /**
     * Backfills missing provider identifiers and makes chunk uniqueness provider-aware.
     *
     * @param sq query executor connected to the RAG datasource
     */
    private void migrateEmbeddingProviderColumn(SimpleQuery sq) {
        String defaultProvider = Constants.getString("ragEmbeddingProvider");
        if (Tools.isEmpty(defaultProvider)) defaultProvider = "openai";

        sq.execute("ALTER TABLE rag_embedding_chunks ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(100)");
        sq.execute("UPDATE rag_embedding_chunks SET embedding_provider = ? WHERE embedding_provider IS NULL OR embedding_provider = ''", defaultProvider.trim().toLowerCase(java.util.Locale.ROOT));
        sq.execute("ALTER TABLE rag_embedding_chunks ALTER COLUMN embedding_provider SET NOT NULL");
        sq.execute("ALTER TABLE rag_embedding_chunks DROP CONSTRAINT IF EXISTS uq_rag_chunk");
        sq.execute("ALTER TABLE rag_embedding_chunks ADD CONSTRAINT uq_rag_chunk UNIQUE (entity_type, entity_id, chunk_index, embedding_provider, embedding_model)");
    }

    /** {@inheritDoc} */
    @Override
    public boolean recreateIndex() {
        return recreateHnswIndex();
    }

    /**
     * Recreates the PostgreSQL HNSW index according to configured distance metric.
     *
     * @return true when the index was recreated successfully
     */
    public boolean recreateHnswIndex() {
        String dsName = getDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping HNSW index recreation");
            return false;
        }

        try {
            String metric = Constants.getString("ragSearchDistanceMetric");
            String indexOps = switch (metric) {
                case "inner_product" -> "vector_ip_ops";
                case "l2" -> "vector_l2_ops";
                default -> "vector_cosine_ops";
            };

            String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_rag_embedding_hnsw ON rag_embedding_chunks" +
                    " USING hnsw (embedding " + indexOps + ")" +
                    " WITH (m = 16, ef_construction = 64)";

            SimpleQuery sq = new SimpleQuery(dsName);
            sq.execute(DROP_HNSW_INDEX_SQL);
            sq.execute(createIndexSql);
            Logger.println(PgVectorStore.class, "RAG HNSW index recreated successfully with distance metric: " + metric);
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error recreating RAG HNSW index: " + e.getMessage());
            return false;
        }
    }

    /**
     * Serializes an embedding in bracketed pgvector text format.
     *
     * @param embedding vector components in dimension order
     * @return comma-separated vector enclosed in brackets
     */
    private String vectorToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parses bracketed pgvector text into vector components.
     *
     * @param vectorStr vector text, possibly null
     * @return parsed vector, or an empty array for null or text shorter than two characters
     */
    private float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.length() < 2) return new float[0];
        // Strip brackets
        String inner = vectorStr.substring(1, vectorStr.length() - 1);
        String[] parts = inner.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * Fetches completed embeddings for content reuse within one source, provider, model, and domain.
     *
     * @param entityType stored source entity type
     * @param entityId source identifier
     * @param embeddingProvider provider that generated the vectors
     * @param embeddingModel model that generated the vectors
     * @param domainId storage domain, including zero for shared Markdown
     * @return vectors keyed by content hash, or an empty map when the datasource or query is unavailable
     */
    @Override
    public Map<String, float[]> getExistingEmbeddingsByHash(String entityType, long entityId, String embeddingProvider, String embeddingModel, int domainId) {
        String dsName = getDataSourceName();
        if (dsName == null) return new java.util.HashMap<>();

        try {
            List<Map.Entry<String, float[]>> entries = new ComplexQuery()
                .setSql(GET_EXISTING_HASH_EMBEDDING)
                .setParams(entityType, entityId, embeddingProvider, embeddingModel, domainId)
                .setDatabase(dsName)
                .list(rs -> {
                    String hash = rs.getString("content_hash");
                    float[] emb = parseVector(rs.getString("embedding_text"));
                    return java.util.Map.entry(hash, emb);
                });

            java.util.Map<String, float[]> result = new java.util.HashMap<>();
            for (Map.Entry<String, float[]> entry : entries) {
                result.put(entry.getKey(), entry.getValue());
            }
            return result;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error fetching existing embeddings for " + entityType + "/" + entityId + ": " + e.getMessage());
            return new java.util.HashMap<>();
        }
    }

    @Override
    public boolean resetDimensions(int dimensions) {
        if (dimensions < 1) {
            Logger.error(PgVectorStore.class, "Invalid RAG embedding dimensions: " + dimensions);
            return false;
        }

        String dsName = getDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping dimension reset");
            return false;
        }

        try {
            SimpleQuery sq = new SimpleQuery(dsName);
            sq.execute(DROP_HNSW_INDEX_SQL);
            sq.execute(DELETE_ALL_DATA_SQL);
            sq.execute("ALTER TABLE rag_embedding_chunks ALTER COLUMN embedding TYPE vector(" + dimensions + ") USING embedding::vector(" + dimensions + ")");
            if (recreateHnswIndex() == false) return false;

            Logger.println(PgVectorStore.class, "RAG embedding data deleted and vector dimensions updated to " + dimensions);
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error resetting RAG vector dimensions: " + e.getMessage());
            return false;
        }
    }
}
