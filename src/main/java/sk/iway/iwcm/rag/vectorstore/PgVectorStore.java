package sk.iway.iwcm.rag.vectorstore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.rag.pgvector.EmbeddingChunkStatus;
import sk.iway.iwcm.rag.pgvector.RagJpaConfig;

/**
 * PgVector implementation of VectorStore.
 * Uses native SQL via JDBC to leverage pgvector operators (cosine distance <=>).
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
            status          VARCHAR(20) NOT NULL DEFAULT '" + EmbeddingChunkStatus.COMPLETED.name() + "',
            error_message   VARCHAR(500),
            create_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT uq_rag_chunk UNIQUE (entity_type, entity_id, chunk_index, embedding_model)
        )
        """.formatted(DIMENSION_PLACEHOLDER);

    private static final String CREATE_ENTITY_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_entity ON rag_embedding_chunks (entity_type, entity_id)";

    private static final String CREATE_DOMAIN_LANG_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_domain_lang ON rag_embedding_chunks (domain_id, language, status)";

    private static final String DROP_HNSW_INDEX_SQL =
        "DROP INDEX IF EXISTS idx_rag_embedding_hnsw";

    private static final String UPSERT_SQL =
        "INSERT INTO rag_embedding_chunks" +
        "    (entity_type, entity_id, chunk_index, chunk_text, content_hash, embedding," +
        "     embedding_model, dimensions, language, domain_id, status, create_date)" +
        " VALUES (?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?, '" + EmbeddingChunkStatus.COMPLETED.name() + "', CURRENT_TIMESTAMP)" +
        " ON CONFLICT (entity_type, entity_id, chunk_index, embedding_model)" +
        " DO UPDATE SET chunk_text = EXCLUDED.chunk_text, content_hash = EXCLUDED.content_hash," +
        "              embedding = EXCLUDED.embedding, status = '" + EmbeddingChunkStatus.COMPLETED.name() + "'," +
        "              error_message = NULL";

    private static final String DELETE_BY_ENTITY_SQL =
        "DELETE FROM rag_embedding_chunks WHERE entity_type = ? AND entity_id = ?";

    private static final String MARK_ERROR_SQL =
        "UPDATE rag_embedding_chunks" +
        " SET status = '" + EmbeddingChunkStatus.ERROR.name() + "', error_message = ?" +
        " WHERE entity_type = ? AND entity_id = ? AND embedding_model = ?";

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

    private static final String DROP_TABLE_SQL =
        "DROP TABLE IF EXISTS rag_embedding_chunks";

    @Override
    public void store(String entityType, long entityId, int chunkIndex, String chunkText,
                      String contentHash, float[] embedding, String embeddingModel,
                      int dimensions, String language, Integer domainId) {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return;

        try {
            new SimpleQuery(dsName).execute(UPSERT_SQL,
                entityType, entityId, chunkIndex, chunkText, contentHash,
                vectorToString(embedding), embeddingModel, dimensions, language, domainId);
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error storing embedding for " + entityType + "/" + entityId + "/" + chunkIndex + ": " + e.getMessage());
        }
    }

    @Override
    public void deleteByEntity(String entityType, long entityId) {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return;

        try {
            new SimpleQuery(dsName).execute(DELETE_BY_ENTITY_SQL, entityType, entityId);
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error deleting embeddings for " + entityType + "/" + entityId + ": " + e.getMessage());
        }
    }

    @Override
    public void markError(String entityType, long entityId, String embeddingModel, String errorMessage) {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (Tools.isEmpty(dsName)) return;

        try {
            String truncatedMessage = errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500) : errorMessage;
            new SimpleQuery(dsName).execute(MARK_ERROR_SQL,
                truncatedMessage, entityType, entityId, embeddingModel);
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error marking chunks as ERROR for " + entityType + "/" + entityId + ": " + e.getMessage());
        }
    }

    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, String embeddingModel, Integer domainId, String language, int limit) {
        String dsName = RagJpaConfig.getRagDataSourceName();
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

        if (domainId != null) {
            sql.append(" AND domain_id = ?");
            params.add(domainId);
        }
        if (Tools.isNotEmpty(language)) {
            sql.append(" AND language = ?");
            params.add(language);
        }
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
    public boolean isAvailable() {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return false;

        try {
            new SimpleQuery(dsName).forInt("SELECT 1 FROM rag_embedding_chunks LIMIT 1");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean initializeSchema() {
        String dsName = RagJpaConfig.getRagDataSourceName();
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
        String dsName = RagJpaConfig.getRagDataSourceName();
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
        String dsName = RagJpaConfig.getRagDataSourceName();
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
    public boolean dropSchema() {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping schema drop");
            return false;
        }

        try {
            SimpleQuery sq = new SimpleQuery(dsName);
            sq.execute(DROP_TABLE_SQL);
            Logger.println(PgVectorStore.class, "RAG pgvector table dropped successfully");
            return true;
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error dropping RAG pgvector table: " + e.getMessage());
            return false;
        }
    }
}