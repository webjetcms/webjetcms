package sk.iway.iwcm.system.audit.jpa;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

/**
 * Bean to handle log level for package for DataTable
 */
@Getter
@Setter
public class LogLevelBean {

    public LogLevelBean() {}

    public LogLevelBean(Long id, String packageName, String logLevel) {
		this.id = id;
        this.packageName = packageName;
        this.logLevel = logLevel.toUpperCase();
    }

	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title = "audit_log_level.package_name"
    )
	private String packageName;

	@DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "audit_log_level.log_level",
        editor = {
			@DataTableColumnEditor(
				options = {
					@DataTableColumnEditorAttr(key = "DEBUG", value = "DEBUG"),
					@DataTableColumnEditorAttr(key = "NORMAL", value = "NORMAL"),
					@DataTableColumnEditorAttr(key = "ERROR", value = "ERROR"),
					@DataTableColumnEditorAttr(key = "INFO", value = "INFO"),
					@DataTableColumnEditorAttr(key = "TRACE", value = "TRACE"),
					@DataTableColumnEditorAttr(key = "WARN", value = "WARN"),
				}
			)
		}
    )
	private String logLevel;

	@DataTableColumn(
		inputType = DataTableColumnType.CHECKBOX,
		title = "audit_log_level.save_in_db_label",
		hidden = true,
		editor = {
			@DataTableColumnEditor(
				attr = {
					@DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "before"),
					@DataTableColumnEditorAttr(key = "data-dt-field-headline", value = "[[#{audit_log_level.save_in_db}]]")
				}
			)
		}
	)
	private boolean saveIntoDB;

	public String getFullLog() {
		return packageName + "=" + logLevel;
	}
}
