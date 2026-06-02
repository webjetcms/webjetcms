package sk.iway.iwcm.rag.jpa;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.rag.RagIndexAction;
import sk.iway.iwcm.rag.service.RagEntityType;
import sk.iway.iwcm.system.adminlog.AuditEntityListener;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

/**
 * Entity representing an item in the RAG indexing queue.
 * Documents are added here on save/delete events and processed by a cron task.
 */
@Entity
@Table(name = "rag_index_queue")
@Getter
@Setter
@EntityListeners(AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_SEARCH)
public class IndexQueueEntity {

    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "WJGen_rag_index_queue")
    @TableGenerator(name = "WJGen_rag_index_queue", pkColumnValue = "rag_index_queue")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private RagEntityType entityType;

    @Column(name = "entity_id")
    private Integer entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private RagIndexAction action;

    @Column(name = "domain_id")
    private Integer domainId;

    @Column(name = "create_date")
    private Date createDate;
}
