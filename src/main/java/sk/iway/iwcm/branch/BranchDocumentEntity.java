package sk.iway.iwcm.branch;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Typed working content, isolated from the LIVE persistence context. */
@Entity
@Table(name="branch_documents")
@Cacheable(false)
@Getter @Setter
public class BranchDocumentEntity extends BranchRow
{
    @Column(name = "doc_id")
    private Integer docId;
    @Column(name = "title")
    private String title;
    @Lob
    @Column(name = "data")
    private String data;
    @Lob
    @Column(name = "data_asc")
    private String dataAsc;
    @Column(name = "external_link")
    private String externalLink;
    @Lob
    @Column(name = "navbar")
    private String navbar;
    @Column(name = "date_created")
    private java.sql.Timestamp dateCreated;
    @Column(name = "publish_start")
    private java.sql.Timestamp publishStart;
    @Column(name = "publish_end")
    private java.sql.Timestamp publishEnd;
    @Column(name = "author_id")
    private Integer authorId;
    @Column(name = "group_id")
    private Integer groupId;
    @Column(name = "temp_id")
    private Integer tempId;
    @Column(name = "views_total")
    private Integer viewsTotal;
    @Column(name = "views_month")
    private Integer viewsMonth;
    @Column(name = "searchable")
    private Integer searchable;
    @Column(name = "available")
    private Integer available;
    @Column(name = "cacheable")
    private Integer cacheable;
    @Column(name = "file_name")
    private String fileName;
    @Column(name = "file_change")
    private java.sql.Timestamp fileChange;
    @Column(name = "sort_priority")
    private Integer sortPriority;
    @Column(name = "header_doc_id")
    private Integer headerDocId;
    @Column(name = "menu_doc_id")
    private Integer menuDocId;
    @Column(name = "footer_doc_id")
    private Integer footerDocId;
    @Column(name = "password_protected")
    private String passwordProtected;
    @Lob
    @Column(name = "html_head")
    private String htmlHead;
    @Lob
    @Column(name = "html_data")
    private String htmlData;
    @Column(name = "perex_place")
    private String perexPlace;
    @Column(name = "perex_image")
    private String perexImage;
    @Column(name = "perex_group")
    private String perexGroup;
    @Column(name = "show_in_menu")
    private Integer showInMenu;
    @Column(name = "event_date")
    private java.sql.Timestamp eventDate;
    @Column(name = "virtual_path")
    private String virtualPath;
    @Column(name = "sync_id")
    private Integer syncId;
    @Column(name = "sync_status")
    private Integer syncStatus;
    @Column(name = "logon_page_doc_id")
    private Integer logonPageDocId;
    @Column(name = "right_menu_doc_id")
    private Integer rightMenuDocId;
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
    @Column(name = "field_g")
    private String fieldG;
    @Column(name = "field_h")
    private String fieldH;
    @Column(name = "field_i")
    private String fieldI;
    @Column(name = "field_j")
    private String fieldJ;
    @Column(name = "field_k")
    private String fieldK;
    @Column(name = "field_l")
    private String fieldL;
    @Column(name = "disable_after_end")
    private Integer disableAfterEnd;
    @Column(name = "forum_count")
    private Integer forumCount;
    @Column(name = "field_m")
    private String fieldM;
    @Column(name = "field_n")
    private String fieldN;
    @Column(name = "field_o")
    private String fieldO;
    @Column(name = "field_p")
    private String fieldP;
    @Column(name = "field_q")
    private String fieldQ;
    @Column(name = "field_r")
    private String fieldR;
    @Column(name = "field_s")
    private String fieldS;
    @Column(name = "field_t")
    private String fieldT;
    @Column(name = "require_ssl")
    private Integer requireSsl;
    @Column(name = "root_group_l1")
    private Integer rootGroupL1;
    @Column(name = "root_group_l2")
    private Integer rootGroupL2;
    @Column(name = "root_group_l3")
    private Integer rootGroupL3;
    @Column(name = "temp_field_a_docid")
    private Integer tempFieldADocid;
    @Column(name = "temp_field_b_docid")
    private Integer tempFieldBDocid;
    @Column(name = "temp_field_c_docid")
    private Integer tempFieldCDocid;
    @Column(name = "temp_field_d_docid")
    private Integer tempFieldDDocid;
    @Column(name = "show_in_navbar")
    private Integer showInNavbar;
    @Column(name = "show_in_sitemap")
    private Integer showInSitemap;
    @Column(name = "logged_show_in_menu")
    private Integer loggedShowInMenu;
    @Column(name = "logged_show_in_navbar")
    private Integer loggedShowInNavbar;
    @Column(name = "logged_show_in_sitemap")
    private Integer loggedShowInSitemap;
    @Column(name = "url_inherit_group")
    private Integer urlInheritGroup;
    @Column(name = "generate_url_from_title")
    private Integer generateUrlFromTitle;
    @Column(name = "editor_virtual_path")
    private String editorVirtualPath;
    @Column(name = "publish_after_start")
    private Integer publishAfterStart;
    @Column(name = "planned_media_initialized")
    private Integer plannedMediaInitialized;
    @Column(name = "follow_links")
    private Integer followLinks;
}
