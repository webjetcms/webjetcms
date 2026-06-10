package sk.iway.iwcm.rag.vectorstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.pgvector.PgvectorJpaConfig;
import sk.iway.iwcm.rag.service.RagEntityType;

/**
 * PgVector implementation of VectorStore.
 * Handles ONLY the embedding (vector) column via native SQL using pgvector operators.
 * Entity CRUD operations are handled by EmbeddingChunkRepository (JPA).
 * Connects to the RAG datasource (primary PgSQL or secondary rag_jpa).
 */
@Service
public class PgVectorStore implements VectorStore {

    private static final String DIMENSION_PLACEHOLDER = "{DIMENSION_PLACEHOLDER}";

    private static final String CREATE_EXTENSION_SQL = "CREATE EXTENSION IF NOT EXISTS vector";

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS rag_embedding_chunks (
            id              BIGSERIAL PRIMARY KEY,
            entity_type     VARCHAR(100) NOT NULL,
            entity_id       BIGINT NOT NULL,
            chunk_index     INT NOT NULL,
            chunk_text      TEXT NOT NULL,
            content_hash    VARCHAR(64) NOT NULL,
            embedding       vector(%s),
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
            CONSTRAINT uq_rag_chunk UNIQUE (entity_type, entity_id, chunk_index, embedding_model)
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
        "UPDATE rag_embedding_chunks SET embedding = ?::vector WHERE id = ?";

    /**
     * Builds SEARCH_SQL dynamically based on configured distance metric.
     * Supports: cosine (1 - <=>), inner_product (negated <#>), l2 (normalized <->)
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
               " AND embedding_model = ?";
    }

    private static final String GET_EXISTING_HASH_EMBEDDING =
        "SELECT content_hash, embedding::text AS embedding_text FROM rag_embedding_chunks " +
        "WHERE entity_type = ? AND entity_id = ? AND embedding_model = ? AND status = '" + EmbeddingChunkStatus.COMPLETED.name() + "'";

    private static final String DELETE_MODEL_DATA_SQL =
        "DELETE FROM rag_embedding_chunks WHERE embedding_model = ?";

    private static final String FTS_SEARCH_SQL_PREFIX =
        "SELECT id, entity_type, entity_id, chunk_index, chunk_text, " +
        "ts_rank_cd(to_tsvector('simple', chunk_text), websearch_to_tsquery('simple', ?)) AS similarity " +
        "FROM rag_embedding_chunks " +
        "WHERE status = '" + EmbeddingChunkStatus.COMPLETED.name() + "' " +
        "AND embedding_model = ? " +
        "AND to_tsvector('simple', chunk_text) @@ websearch_to_tsquery('simple', ?)";

    private static final String FTS_ILIKE_SQL_PREFIX =
        "SELECT id, entity_type, entity_id, chunk_index, chunk_text, " +
        "CASE WHEN position(lower(?) in lower(chunk_text)) > 0 THEN 1.0 ELSE 0.0 END AS similarity " +
        "FROM rag_embedding_chunks " +
        "WHERE status = '" + EmbeddingChunkStatus.COMPLETED.name() + "' " +
        "AND embedding_model = ? " +
        "AND chunk_text ILIKE ?";

    @Override
    public void updateEmbedding(Long id, float[] embedding) {
        if (id == null || embedding == null || embedding.length == 0) return;

        String dsName = PgvectorJpaConfig.getRagDataSourceName();
        if (dsName == null) return;

        try {
            new SimpleQuery(dsName).execute(UPDATE_EMBEDDING_SQL, vectorToString(embedding), id);
        } catch (Exception e) {
            Logger.error(PgVectorStore.class,
                "Error updating embedding for chunk id " + id + ": " + e.getMessage());
        }
    }

    @Override
    public void updateEmbeddingBatch(List<Long> ids, List<float[]> embeddings) {
        if (ids == null || ids.isEmpty() || embeddings == null || embeddings.isEmpty()) return;
        if (ids.size() != embeddings.size()) {
            throw new IllegalArgumentException("IDs/embeddings count mismatch: ids=" + ids.size() + ", embeddings=" + embeddings.size());
        }

        String dsName = PgvectorJpaConfig.getRagDataSourceName();
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

            new SimpleQuery(dsName).execute(sql, params.toArray());
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error batch updating embeddings, falling back to row-by-row updates: " + e.getMessage());
            for (int i = 0; i < ids.size(); i++) {
                updateEmbedding(ids.get(i), embeddings.get(i));
            }
        }
    }

    private String buildBatchUpdateEmbeddingSql(int rowCount) {
        StringBuilder sql = new StringBuilder("UPDATE rag_embedding_chunks SET embedding = CASE id ");

        for (int i = 0; i < rowCount; i++) {
            sql.append("WHEN ? THEN ?::vector ");
        }

        sql.append("END WHERE id IN (");
        for (int i = 0; i < rowCount; i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        return sql.toString();
    }

    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        String dsName = PgvectorJpaConfig.getRagDataSourceName();
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
        params.add(embeddingModel);

        addScopeFilters(sql, params, entityType, domainId, language);

        addEntityTypeSpecificConditions(sql, entityType, bonusParams);

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
    public List<VectorSearchResult> searchFulltext(String query, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        String dsName = PgvectorJpaConfig.getRagDataSourceName();
        if (dsName == null || Tools.isEmpty(query) || limit <= 0) return new ArrayList<>();

        List<VectorSearchResult> ftsResults = executeFulltextSearch(dsName, query, embeddingModel, entityType, domainId, language, limit, bonusParams);

        boolean hybridFtsUseIlikeFallback = Constants.getBoolean("ragHybridFtsUseIlikeFallback");
        if(bonusParams != null) {
            hybridFtsUseIlikeFallback = Tools.getBooleanValue(
                                            String.valueOf(bonusParams.get("hybridFtsUseIlikeFallback")),
                                            hybridFtsUseIlikeFallback
                                        );
        }

        if (ftsResults.isEmpty() && hybridFtsUseIlikeFallback) {
            return executeIlikeSearch(dsName, query, embeddingModel, entityType, domainId, language, limit, bonusParams);
        }

        return ftsResults;
    }

    private List<VectorSearchResult> executeFulltextSearch(String dsName, String query, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        StringBuilder sql = new StringBuilder(FTS_SEARCH_SQL_PREFIX);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(embeddingModel);
        params.add(query);

        addScopeFilters(sql, params, entityType, domainId, language);

        addEntityTypeSpecificConditions(sql, entityType, bonusParams);

        sql.append(" ORDER BY similarity DESC LIMIT ?");
        params.add(limit);

        return executeSearchQuery(dsName, sql.toString(), params);
    }

    private List<VectorSearchResult> executeIlikeSearch(String dsName, String query, String embeddingModel, RagEntityType entityType, Integer domainId, String language, int limit, Map<String, Object> bonusParams) {
        StringBuilder sql = new StringBuilder(FTS_ILIKE_SQL_PREFIX);
        List<Object> params = new ArrayList<>();
        params.add(query);
        params.add(embeddingModel);
        params.add("%" + query + "%");

        addScopeFilters(sql, params, entityType, domainId, language);

        addEntityTypeSpecificConditions(sql, entityType, bonusParams);

        sql.append(" ORDER BY similarity DESC, id ASC LIMIT ?");
        params.add(limit);

        return executeSearchQuery(dsName, sql.toString(), params);
    }

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

    private void addEntityTypeSpecificConditions(StringBuilder sql, RagEntityType entityType, Map<String, Object> bonusParams) {

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
        // PgVector is available only when the RAG datasource is enabled and reachable.
        return PgvectorJpaConfig.isRagAvailable();
    }

    @Override
    public boolean isAvailableAndInitialized() {
        // Availability checks the datasource; this query verifies that the schema has been initialized.
        if (isAvailable() == false) return false;

        try {
            new SimpleQuery(PgvectorJpaConfig.getRagDataSourceName()).forInt("SELECT 1 FROM rag_embedding_chunks LIMIT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean initializeSchema() {
        String dsName = PgvectorJpaConfig.getRagDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping schema initialization");
            return false;
        }

        try {
            SimpleQuery sq = new SimpleQuery(dsName);
            sq.execute(CREATE_EXTENSION_SQL);
            // Important note: the table creation must be done with the correct dimension count, otherwise the embedding insertions will fail with "vector has wrong number of dimensions" error
            sq.execute( Tools.replace(CREATE_TABLE_SQL, DIMENSION_PLACEHOLDER, Constants.getInt("ragEmbeddingDimensions") + "") );

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

            Logger.println(PgVectorStore.class, "RAG pgvector schema initialized successfully");
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error initializing RAG pgvector schema: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recreates HNSW index according to configured distance metric.
     */
    public boolean recreateHnswIndex() {
        String dsName = PgvectorJpaConfig.getRagDataSourceName();
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
     * Convert a float array to the pgvector string format: [0.1,0.2,0.3]
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
     * Parse pgvector text format "[0.1,0.2,0.3]" back to float[].
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
     * {@inheritDoc}
     * Fetches existing content hashes and their embedding vectors for an entity,
     * allowing the indexer to skip re-embedding unchanged chunks.
     * */
    @Override
    public Map<String, float[]> getExistingEmbeddingsByHash(String entityType, long entityId, String embeddingModel) {
        String dsName = PgvectorJpaConfig.getRagDataSourceName();
        if (dsName == null) return new java.util.HashMap<>();

        try {
            List<Map.Entry<String, float[]>> entries = new ComplexQuery()
                .setSql(GET_EXISTING_HASH_EMBEDDING)
                .setParams(entityType, entityId, embeddingModel)
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
    public boolean deleteModelData(String embeddingModel) {
        String dsName = PgvectorJpaConfig.getRagDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping schema drop");
            return false;
        }

        try {
            SimpleQuery sq = new SimpleQuery(dsName);
            sq.execute(DELETE_MODEL_DATA_SQL, embeddingModel);
            Logger.println(PgVectorStore.class, "RAG pgvector table dropped successfully");
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error dropping RAG pgvector table: " + e.getMessage());
            return false;
        }
    }
}
