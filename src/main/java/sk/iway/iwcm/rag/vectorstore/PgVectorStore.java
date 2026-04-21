package sk.iway.iwcm.rag.vectorstore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.DBPool;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.rag.pgvector.RagJpaConfig;

/**
 * PgVector implementation of VectorStore.
 * Uses native SQL via JDBC to leverage pgvector operators (cosine distance <=>).
 * Connects to the RAG datasource (primary PgSQL or secondary rag_jpa).
 */
@Service
public class PgVectorStore implements VectorStore {

    private static final String CREATE_EXTENSION_SQL = "CREATE EXTENSION IF NOT EXISTS vector";

    private static final String CREATE_TABLE_SQL = """
        CREATE TABLE IF NOT EXISTS rag_embedding_chunks (
            id              BIGSERIAL PRIMARY KEY,
            entity_type     VARCHAR(100) NOT NULL,
            entity_id       BIGINT NOT NULL,
            chunk_index     INT NOT NULL,
            chunk_text      TEXT NOT NULL,
            content_hash    VARCHAR(64) NOT NULL,
            embedding       vector(1536),
            embedding_model VARCHAR(100) NOT NULL,
            dimensions      INT NOT NULL,
            language        VARCHAR(10),
            domain_id       INT,
            status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
            retry_count     INT DEFAULT 0,
            error_message   VARCHAR(500),
            create_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            update_date     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT uq_rag_chunk UNIQUE (entity_type, entity_id, chunk_index, embedding_model)
        )
        """;

    private static final String CREATE_HNSW_INDEX_SQL = """
        CREATE INDEX IF NOT EXISTS idx_rag_embedding_hnsw ON rag_embedding_chunks
            USING hnsw (embedding vector_cosine_ops)
            WITH (m = 16, ef_construction = 64)
        """;

    private static final String CREATE_ENTITY_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_entity ON rag_embedding_chunks (entity_type, entity_id)";

    private static final String CREATE_DOMAIN_LANG_INDEX_SQL =
        "CREATE INDEX IF NOT EXISTS idx_rag_chunk_domain_lang ON rag_embedding_chunks (domain_id, language, status)";

    private static final String UPSERT_SQL = """
        INSERT INTO rag_embedding_chunks
            (entity_type, entity_id, chunk_index, chunk_text, content_hash, embedding,
             embedding_model, dimensions, language, domain_id, status, create_date, update_date)
        VALUES (?, ?, ?, ?, ?, ?::vector, ?, ?, ?, ?, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (entity_type, entity_id, chunk_index, embedding_model)
        DO UPDATE SET chunk_text = EXCLUDED.chunk_text, content_hash = EXCLUDED.content_hash,
                      embedding = EXCLUDED.embedding, status = 'COMPLETED',
                      update_date = CURRENT_TIMESTAMP, retry_count = 0, error_message = NULL
        """;

    private static final String DELETE_BY_ENTITY_SQL =
        "DELETE FROM rag_embedding_chunks WHERE entity_type = ? AND entity_id = ?";

    private static final String SEARCH_SQL = """
        SELECT id, entity_type, entity_id, chunk_index, chunk_text,
               1 - (embedding <=> ?::vector) AS similarity
        FROM rag_embedding_chunks
        WHERE status = 'COMPLETED'
        """;

    @Override
    public void store(String entityType, long entityId, int chunkIndex, String chunkText,
                      String contentHash, float[] embedding, String embeddingModel,
                      int dimensions, String language, Integer domainId) {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return;

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBPool.getConnection(dsName);
            ps = conn.prepareStatement(UPSERT_SQL);
            ps.setString(1, entityType);
            ps.setLong(2, entityId);
            ps.setInt(3, chunkIndex);
            ps.setString(4, chunkText);
            ps.setString(5, contentHash);
            ps.setString(6, vectorToString(embedding));
            ps.setString(7, embeddingModel);
            ps.setInt(8, dimensions);
            ps.setString(9, language);
            if (domainId != null) ps.setInt(10, domainId);
            else ps.setNull(10, java.sql.Types.INTEGER);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error storing embedding for " + entityType + "/" + entityId + "/" + chunkIndex + ": " + e.getMessage());
        } finally {
            try
			{
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
        }
    }

    @Override
    public void deleteByEntity(String entityType, long entityId) {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return;

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBPool.getConnection(dsName);
            ps = conn.prepareStatement(DELETE_BY_ENTITY_SQL);
            ps.setString(1, entityType);
            ps.setLong(2, entityId);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error deleting embeddings for " + entityType + "/" + entityId + ": " + e.getMessage());
        } finally {
             try
			{
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
        }
    }

    @Override
    public List<VectorSearchResult> search(float[] queryEmbedding, Integer domainId, String language, int limit) {
        List<VectorSearchResult> results = new ArrayList<>();
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return results;

        StringBuilder sql = new StringBuilder(SEARCH_SQL);
        List<Object> params = new ArrayList<>();
        params.add(vectorToString(queryEmbedding));

        if (domainId != null) {
            sql.append(" AND domain_id = ?");
            params.add(domainId);
        }
        if (language != null) {
            sql.append(" AND language = ?");
            params.add(language);
        }
        sql.append(" ORDER BY embedding <=> ?::vector LIMIT ?");
        params.add(vectorToString(queryEmbedding));
        params.add(limit);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBPool.getConnection(dsName);
            ps = conn.prepareStatement(sql.toString());

            int idx = 1;
            for (Object param : params) {
                if (param instanceof Integer) ps.setInt(idx, (Integer) param);
                else if (param instanceof String) ps.setString(idx, (String) param);
                else ps.setObject(idx, param);
                idx++;
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                VectorSearchResult result = new VectorSearchResult(
                    rs.getLong("id"),
                    rs.getString("entity_type"),
                    rs.getLong("entity_id"),
                    rs.getInt("chunk_index"),
                    rs.getString("chunk_text"),
                    rs.getDouble("similarity")
                );
                results.add(result);
            }
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error executing vector search: " + e.getMessage());
        } finally {
            try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
        }

        return results;
    }

    @Override
    public boolean isAvailable() {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) return false;

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBPool.getConnection(dsName);
            ps = conn.prepareStatement("SELECT 1 FROM rag_embedding_chunks LIMIT 1");
            rs = ps.executeQuery();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
             try
			{
				if (rs != null)
					rs.close();
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
        }
    }

    @Override
    public void initializeSchema() {
        String dsName = RagJpaConfig.getRagDataSourceName();
        if (dsName == null) {
            Logger.println(PgVectorStore.class, "RAG datasource not available, skipping schema initialization");
            return;
        }

        Connection conn = null;
        try {
            conn = DBPool.getConnection(dsName);
            try (PreparedStatement ps = conn.prepareStatement(CREATE_EXTENSION_SQL)) { ps.execute(); }
            try (PreparedStatement ps = conn.prepareStatement(CREATE_TABLE_SQL)) { ps.execute(); }
            try (PreparedStatement ps = conn.prepareStatement(CREATE_HNSW_INDEX_SQL)) { ps.execute(); }
            try (PreparedStatement ps = conn.prepareStatement(CREATE_ENTITY_INDEX_SQL)) { ps.execute(); }
            try (PreparedStatement ps = conn.prepareStatement(CREATE_DOMAIN_LANG_INDEX_SQL)) { ps.execute(); }
            Logger.println(PgVectorStore.class, "RAG pgvector schema initialized successfully");
        } catch (Exception e) {
            Logger.error(PgVectorStore.class, "Error initializing RAG pgvector schema: " + e.getMessage());
        } finally {
             try
			{
				if (conn != null)
					conn.close();
			}
			catch (Exception ex2)
			{
				sk.iway.iwcm.Logger.error(ex2);
			}
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
}
