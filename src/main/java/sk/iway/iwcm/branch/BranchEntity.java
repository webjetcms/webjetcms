package sk.iway.iwcm.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** A domain-scoped publishing package; content is captured lazily. */
@Entity
@Table(name="branches")
@Cacheable(false)
@Getter @Setter
public class BranchEntity
{
    public static final String DEFAULT_COLOR = "cyan";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "domain_name")
    private String domainName;
    @Column(name = "name")
    private String name;
    @Column(name = "color", length = 16, nullable = false)
    private String color = DEFAULT_COLOR;
    @Lob
    @Column(name = "description")
    private String description;
    @Column(name = "state")
    private String state;
    @Column(name = "revision")
    private Long revision;
    @Column(name = "publish_at")
    private java.sql.Timestamp publishAt;
    @Column(name = "scheduled_by")
    private Integer scheduledBy;
    @Column(name = "created_by")
    private Integer createdBy;
    @Column(name = "updated_by")
    private Integer updatedBy;
    @Column(name = "created_at")
    private java.sql.Timestamp createdAt;
    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;
    @Column(name = "merge_started_at")
    private java.sql.Timestamp mergeStartedAt;
    @Column(name = "merge_finished_at")
    private java.sql.Timestamp mergeFinishedAt;
    @Lob
    @Column(name = "last_error")
    private String lastError;
}
