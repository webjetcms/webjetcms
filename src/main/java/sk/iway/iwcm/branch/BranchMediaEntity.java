package sk.iway.iwcm.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Typed working content, isolated from the LIVE persistence context. */
@Entity
@Table(name="branch_media")
@Cacheable(false)
@Getter @Setter
public class BranchMediaEntity extends BranchRow
{
    @Column(name = "media_id")
    private Integer mediaId;
    @Column(name = "media_fk_id")
    private Integer mediaFkId;
    @Column(name = "media_fk_table_name")
    private String mediaFkTableName;
    @Column(name = "media_title_sk")
    private String mediaTitleSk;
    @Column(name = "media_title_cz")
    private String mediaTitleCz;
    @Column(name = "media_title_de")
    private String mediaTitleDe;
    @Column(name = "media_title_en")
    private String mediaTitleEn;
    @Column(name = "media_link")
    private String mediaLink;
    @Column(name = "media_thumb_link")
    private String mediaThumbLink;
    @Column(name = "media_group")
    private String mediaGroup;
    @Lob
    @Column(name = "media_info_sk")
    private String mediaInfoSk;
    @Lob
    @Column(name = "media_info_cz")
    private String mediaInfoCz;
    @Lob
    @Column(name = "media_info_de")
    private String mediaInfoDe;
    @Lob
    @Column(name = "media_info_en")
    private String mediaInfoEn;
    @Column(name = "media_sort_order")
    private Integer mediaSortOrder;
    @Column(name = "last_update")
    private java.sql.Timestamp lastUpdate;
    @Column(name = "domain_id")
    private Integer domainId;
    @Column(name = "field_a")
    private String fieldA;
    @Column(name = "field_b")
    private String fieldB;
    @Column(name = "field_c")
    private String fieldC;
    @Column(name = "field_d")
    private String fieldD;
    @Column(name = "field_e")
    private String fieldE;
    @Column(name = "field_f")
    private String fieldF;
    @Lob
    @Column(name = "group_ids_snapshot")
    private String groupIdsSnapshot;
}
