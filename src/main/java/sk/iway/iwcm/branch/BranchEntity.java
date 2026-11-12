package sk.iway.iwcm.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

/** A domain-scoped publishing package; content is captured lazily. */
@Entity
@Table(name="branches")
@Cacheable(false)
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@sk.iway.iwcm.system.adminlog.EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CLIENT_SPECIFIC)
@Getter @Setter
public class BranchEntity
{
    public static final String DEFAULT_COLOR = "cyan";

    /** Returns the current routing name without persisting a second domain identity. */
    public String getDomainName() {
        return domainId == null ? null : BranchDomains.name(domainId);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;
    @Column(name = "domain_id", nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private Integer domainId;
    @Column(name = "name")
    @DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, title = "branches.name.js")
    @NotBlank
    @Size(max = 255)
    private String name;
    @Column(name = "color", length = 16, nullable = false)
    @DataTableColumn(inputType = DataTableColumnType.RADIO, title = "admin.dashboard.shortcutColor.js")
    @NotNull
    @Size(max = 16)
    private String color = DEFAULT_COLOR;
    @Lob
    @Column(name = "description")
    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "branches.description.js", visible = false)
    private String description;
    @Column(name = "state")
    @DataTableColumn(inputType = DataTableColumnType.SELECT, title = "branches.table.state", hiddenEditor = true)
    private String state;
    @Column(name = "revision")
    @DataTableColumn(hidden = true, hiddenEditor = true)
    private Long revision;
    @Column(name = "publish_at")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "branches.merge.publishAt.js", hiddenEditor = true, visible = false)
    private java.sql.Timestamp publishAt;
    @Column(name = "scheduled_by")
    @DataTableColumn(hidden = true, hiddenEditor = true)
    private Integer scheduledBy;
    @Column(name = "created_by")
    @DataTableColumn(hidden = true, hiddenEditor = true)
    private Integer createdBy;
    @Column(name = "updated_by")
    private Integer updatedBy;
    @Column(name = "created_at")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "branches.table.createdAt", hiddenEditor = true, visible = false)
    private java.sql.Timestamp createdAt;
    @Column(name = "updated_at")
    @DataTableColumn(inputType = DataTableColumnType.DATETIME, title = "webpages.last_update", editor = @DataTableColumnEditor(type = "hidden"))
    private java.sql.Timestamp updatedAt;
    @Column(name = "merge_started_at")
    @DataTableColumn(hidden = true, hiddenEditor = true)
    private java.sql.Timestamp mergeStartedAt;
    @Column(name = "merge_finished_at")
    @DataTableColumn(hidden = true, hiddenEditor = true)
    private java.sql.Timestamp mergeFinishedAt;
    @Lob
    @Column(name = "last_error")
    @DataTableColumn(hidden = true, hiddenEditor = true)
    private String lastError;
}
