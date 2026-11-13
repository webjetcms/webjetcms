package sk.iway.iwcm.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Typed working content, isolated from the LIVE persistence context. */
@Entity
@Table(name="branch_files")
@Cacheable(false)
@Getter @Setter
public class BranchFileEntity extends BranchRow
{
    @Column(name = "entry_kind")
    private String entryKind;
    @Column(name = "logical_path")
    private String logicalPath;
    @Column(name = "path_key")
    private String pathKey;
    @Column(name = "base_content_hash")
    private String baseContentHash;
    @Column(name = "work_content_hash")
    private String workContentHash;
    @Column(name = "size_bytes")
    private Long sizeBytes;
    @Column(name = "mime_type")
    private String mimeType;
}
