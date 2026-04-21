package sk.iway.iwcm.rag.pgvector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a chunk of text with its embedding vector stored in PostgreSQL (pgvector).
 * The 'embedding' column is of type vector(1536) in PgSQL and is handled via native SQL,
 * not mapped as a JPA field (JPA/EclipseLink does not support pgvector types natively).
 */
@Entity
@Table(name = "rag_embedding_chunks")
@Getter
@Setter
public class EmbeddingChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    // embedding column is NOT mapped here - it's vector(1536) type handled via native SQL
    // Use PgVectorStore for embedding operations

    @Column(name = "embedding_model", nullable = false, length = 100)
    private String embeddingModel;

    @Column(name = "dimensions", nullable = false)
    private Integer dimensions;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "domain_id")
    private Integer domainId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "create_date")
    private Date createDate;

    @Column(name = "update_date")
    private Date updateDate;
}
