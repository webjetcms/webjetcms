package sk.iway.iwcm.system.audit.jpa;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Entity
@Table(name = "_adminlog_")
@Getter
@Setter
public class AuditLogEntity {

	public AuditLogEntity() {
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_webjet_adminlog")
	@Column(name = "log_id")
	@DataTableColumn(inputType = DataTableColumnType.ID, tab="main")
	private Long id;

	@Column(name = "create_date")
	@DataTableColumn(inputType = DataTableColumnType.DATETIME, tab="main", renderFormat = "dt-format-date-time", editor = {
			@DataTableColumnEditor(type = "datetime", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private Timestamp createDate;

	@Column(name = "log_type")
	@DataTableColumn(inputType = DataTableColumnType.SELECT, tab="main", renderFormat = "dt-format-select", editor = {
			@DataTableColumnEditor(type = "select", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private Integer logType;

	@Column(name = "user_id")
	// @DataTableColumn(inputType = DataTableColumnType.TEXT)
	private Integer userId;

	@Transient
	@DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", renderFormat = "dt-format-text", orderable = false, editor = {
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

	@Size(max = 1000)
	@Column(name = "description")
	@DataTableColumn(inputType = DataTableColumnType.OPEN_EDITOR, renderFormat = "dt-format-text", tab="description", editor = {
			@DataTableColumnEditor(type = "textarea", attr = {
					//disabled nedame, z UX hladiska je lepsie, ked tam viem dat kurzor a selectovat text z napr. SQL chyb @DataTableColumnEditorAttr(key = "disabled", value = "disabled"),
					@DataTableColumnEditorAttr(key = "class", value = "textarea-code") }) })
	private String description;

	@Column(name = "sub_id1")
	@DataTableColumn(inputType = DataTableColumnType.NUMBER, tab="main", renderFormat = "dt-format-number",
		editor = { @DataTableColumnEditor(attr = {	@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) }
	)
	private Integer subId1;

	@Column(name = "sub_id2")
	// @DataTableColumn(inputType = DataTableColumnType.TEXT_NUMBER)
	private Integer subId2;

	@Size(max = 255)
	@Column(name = "ip")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", renderFormat = "dt-format-text", editor = {
			@DataTableColumnEditor(type = "text", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private String ip;

	@Size(max = 255)
	@Column(name = "hostname")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, tab="main", renderFormat = "dt-format-text", editor = {
			@DataTableColumnEditor(type = "text", attr = {
					@DataTableColumnEditorAttr(key = "disabled", value = "disabled") }) })
	private String hostname;
}
