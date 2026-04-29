package sk.iway.iwcm.rag.pgvector;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.datatable.BaseEditorFields;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Entity representing a chunk of text with its embedding vector stored in PostgreSQL (pgvector).
 * The 'embedding' column is of type vector(1536) in PgSQL and is handled via native SQL,
 * not mapped as a JPA field (JPA/EclipseLink does not support pgvector types natively).
 */
@Entity
@Table(name = "rag_embedding_chunks")
@Getter
@Setter
public class EmbeddingChunkEntity extends BaseEditorFields {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_rag_embedding_chunks")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 100)
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "settings.embedding-chunks.entityType",
        tab = "main",
        renderFormat = "dt-format-select",
        visible = false,
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") },
                options = {
                    @DataTableColumnEditorAttr(key = "DOCUMENT", value = "DOCUMENT")
                }
            )
        }
    )
    private RagEntityType entityType;

    @Column(name = "entity_id", nullable = false)
    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "settings.embedding-chunks.entityId",
        tab = "main"
    )
    private Long entityId;

    @Column(name = "chunk_index", nullable = false)
    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "settings.embedding-chunks.chunkIndex",
        tab = "main"
    )
    private Integer chunkIndex;

    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "settings.embedding-chunks.chunkText",
        tab = "main",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String chunkText;

    @Column(name = "content_hash", nullable = false, length = 64)
    @DataTableColumn(
        inputType = DataTableColumnType.HIDDEN,
        title = "components.rag.content_hash",
        tab = "main"
    )
    private String contentHash;

    // embedding column is NOT mapped here - it's vector(1536) type handled via native SQL
    // Use PgVectorStore for embedding operations

    @Column(name = "embedding_model", nullable = false, length = 100)
    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "settings.embedding-chunks.embeddingModel",
        tab = "main",
        visible = false
    )
    private String embeddingModel;

    @Column(name = "dimensions", nullable = false)
    @DataTableColumn(
        inputType = DataTableColumnType.DISABLED,
        title = "settings.embedding-chunks.dimensions",
        tab = "main",
        visible = false
    )
    private Integer dimensions;

    @Column(name = "language", length = 10)
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "settings.embedding-chunks.language",
        tab = "main",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        }
    )
    private String language;

    @Column(name = "domain_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer domainId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "settings.embedding-chunks.status",
        tab = "main",
        renderFormat = "dt-format-select",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") },
                options = {
                    @DataTableColumnEditorAttr(key = "PENDING", value = "PENDING"),
                    @DataTableColumnEditorAttr(key = "COMPLETED", value = "COMPLETED"),
                    @DataTableColumnEditorAttr(key = "ERROR", value = "ERROR")
                }
            )
        }
    )
    private EmbeddingChunkStatus status = EmbeddingChunkStatus.PENDING;

    @Column(name = "error_message", length = 500)
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "settings.embedding-chunks.errorMessage",
        tab = "main",
        editor = {
            @DataTableColumnEditor(
                attr = { @DataTableColumnEditorAttr(key = "disabled", value = "disabled") }
            )
        },
        visible = false
    )
    private String errorMessage;

    @Column(name = "create_date")
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "settings.embedding-chunks.createDate",
        tab = "main",
        renderFormat = "dt-format-date-time"
    )
    private Date createDate;
}
