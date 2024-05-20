package sk.iway.iwcm.system.audit.jpa;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

@Entity
@Table(name = "adminlog_notify")
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_ADMINLOG_NOTIFY)
@Getter
@Setter
public class AuditNotifyEntity {

	public AuditNotifyEntity() {
		//
	}

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_adminlog_notify")
	@Column(name = "adminlog_notify_id")
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@Column(name = "adminlog_type")
	@DataTableColumn(inputType = { DataTableColumnType.OPEN_EDITOR,
			DataTableColumnType.SELECT }, renderFormat = "dt-format-select", editor = {
					@DataTableColumnEditor(type = "select") })
	private Integer adminlogType;

	@Size(max = 255)
	@Column(name = "text")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, renderFormat = "dt-format-text", editor = {
			@DataTableColumnEditor(type = "text") })
	private String text;

	@Size(max = 255)
	@Column(name = "email")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, renderFormat = "dt-format-mail", editor = {
			@DataTableColumnEditor(type = "text") })
	private String email;
}
