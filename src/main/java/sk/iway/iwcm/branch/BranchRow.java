package sk.iway.iwcm.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Shared metadata of a typed working row. */
@MappedSuperclass
@Getter @Setter
public abstract class BranchRow
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "row_id")
    private Long rowId;
    @Column(name = "branch_id")
    private Long branchId;
    @Column(name = "operation")
    private String operation;
    @Column(name = "base_exists")
    private Boolean baseExists;
    @Lob
    @Column(name = "base_snapshot")
    private String baseSnapshot;
    @Column(name = "base_hash")
    private String baseHash;
    @Column(name = "updated_by")
    private Integer updatedBy;
    @Column(name = "updated_at")
    private java.sql.Timestamp updatedAt;
    @Lob
    @Column(name = "resolved_snapshot")
    private String resolvedSnapshot;
    @Column(name = "resolved_choice")
    private String resolvedChoice;
    @Column(name = "resolved_live_hash")
    private String resolvedLiveHash;
    @Column(name = "resolved_work_hash")
    private String resolvedWorkHash;
    @Column(name = "apply_state")
    private String applyState;
    @Column(name = "apply_phase")
    private String applyPhase;
    @Column(name = "applied_hash")
    private String appliedHash;
    @Lob
    @Column(name = "apply_error")
    private String applyError;
}
