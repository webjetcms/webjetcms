package sk.iway.iwcm.components.configuration.model;

import java.util.Date;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Entity
@Table(name = "_conf_prepared_")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CONF_UPDATE)
public class ConfPreparedEntity {

    public ConfPreparedEntity(){}

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_webjet_conf_prepared")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    /*planovane a historicke nechceme vediet editovat @NotBlank
    @Column(name="name")
    @DataTableColumn(
        inputType=DataTableColumnType.OPEN_EDITOR,
        renderFormat = "dt-format-text",
        title = "admin.conf_editor.name"
    )*/
    String name;

    @NotBlank
    @Column(name="value")
    @DataTableColumn(
        inputType=DataTableColumnType.TEXT,
        renderFormat = "dt-format-text-wrap",
        title = "admin.conf_editor.value"
    )
    String value;

    @NotNull
    @Column(name = "date_changed")
    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        renderFormat = "dt-format-date-time",
        title = "admin.conf_editor.date_change"
    )
    private Date dateChanged;

    @NotNull
    @Column(name = "date_prepared")
    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        renderFormat = "dt-format-date-time",
        title = "admin.conf_editor.change_from"
    )
    private Date datePrepared;

    @Column(name = "user_id")
    Integer userId;

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
}
