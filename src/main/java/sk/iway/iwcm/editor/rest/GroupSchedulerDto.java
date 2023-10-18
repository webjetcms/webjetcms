package sk.iway.iwcm.editor.rest;

import java.util.Date;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Entity
@Table(name="groups_scheduler")
@Getter
@Setter
public class GroupSchedulerDto {

    @Id
    @Column(name = "schedule_id")
    @DataTableColumn(
        inputType = DataTableColumnType.ID,
        renderFormat = "dt-format-selector",
        title = "editor.cell.id")
    private Long id;

    @Column(name = "group_name")
    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "[[#{editor.directory_name}]]")
    private String groupName;

    @Column(name = "save_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "editor.group_schedule.save_date")
    private Date saveDate;

    @Column(name = "when_to_publish")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "editor.group_schedule.when_to_publish")
    private Date whenToPublish;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "group_id")
    private Integer groupId;

    @Column(name = "internal")
    private boolean internal;

    @Column(name = "parent_group_id")
    private Integer parentGroupId;

    @Column(name = "navbar")
    private String navbar;

    @Column(name = "default_doc_id")
    private Integer defaultDocId;

    @Column(name = "temp_id")
    private Integer tempId;

    @Column(name = "sort_priority")
    private Integer sortPriority;

    @Column(name = "password_protected")
    private String passwordProtected;

    @Column(name = "url_dir_name")
    private String urlDirName;

    @Column(name = "sync_id")
    private Integer syncId;

    @Column(name = "sync_status")
    private Integer syncStatus;

    @Column(name = "html_head")
    private String htmlHead;

    @Column(name = "logon_page_doc_id")
    private Integer logonPageDocId;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "new_page_docid_template")
    private Integer newPageDocidTemplate;

    @Column(name = "install_name")
    private String installName;

    @Column(name = "field_a")
    private String fieldA;

    @Column(name = "field_b")
    private String fieldB;

    @Column(name = "field_c")
    private String fieldC;

    @Column(name = "field_d")
    private String fieldD;

    @Column(name = "link_group_id")
    private Integer linkGroupId;

    @Column(name = "lng")
    private String lng;

    @Column(name = "hidden_in_admin")
    private Integer hiddenInAdmin;

    @Column(name = "force_group_template")
    private boolean forceGroupTemplate;

    // ** Pridane atributy - ticket 54205  ** //

    //Not logged
    @Column(name = "menu_type")
    private Integer menuType;

    @Column(name = "show_in_navbar")
    private Integer showInNavbar;

    @Column(name = "show_in_sitemap")
    private Integer showInSitemap;

    //Logged
    @Column(name = "logged_menu_type")
    private Integer loggedMenuType;

    @Column(name = "logged_show_in_navbar")
    private Integer loggedShowInNavbar;

    @Column(name = "logged_show_in_sitemap")
    private Integer loggedShowInSitemap;

    public boolean getInternal() {
        return internal;
    }

    public boolean getForceGroupTemplate() {
        return forceGroupTemplate;
    }

    @Transient
	@DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", renderFormat = "dt-format-text", title="components.audit_log.user_full_name", orderable = false, editor = {
			@DataTableColumnEditor(type = "text", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private String userFullName;

	public String getUserFullName() {
		if (userFullName == null && userId != null && userId.intValue()>0) {
			UserDetails user = UsersDB.getUserCached(userId.intValue());
			if (user!=null)	userFullName = user.getFullName();
			else userFullName = "";
		}
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}

    @Column(name = "date_published")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title = "groupedit.publish.realPublishDate")
    private Date datePublished;
}