package sk.iway.iwcm.system.cron;

import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

import jakarta.validation.constraints.NotBlank;

import static sk.iway.iwcm.Tools.isEmpty;

/**
 * CronTask.java
 * <p>
 * Encapsulates a cron task details in a java bean fashion
 *
 * @author $Author: marosurbanec $
 * @version $Revision: 1.3 $
 * @Title webjet7
 * @Company Interway s.r.o. (www.interway.sk)
 * @Copyright Interway s.r.o. (c) 2001-2010
 * @created Date: 9.7.2010 15:52:55
 * @modified $Date: 2004/08/16 06:26:11 $
 */
public class CronTask {
	@DataTableColumn(inputType = DataTableColumnType.ID)
	private Long id;

	@DataTableColumn(
			inputType = {DataTableColumnType.OPEN_EDITOR},
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.task_name}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String taskName = "";

	@NotBlank
	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.task}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String task = "";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.param}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text",
							attr = @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
					)
			}
	)
	private String params = "";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.year}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String years = "*";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.day_in_month}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String daysOfMonth = "*";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.day_in_week}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String daysOfWeek = "*";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.month}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String months = "*";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.hour}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String hours = "*";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.minute}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text"
					)
			}
	)
	private String minutes = "0";

	@DataTableColumn(
			renderFormat = "dt-format-text-wrap",
			title = "[[#{components.cron_task.second}]]",
			editor = {
					@DataTableColumnEditor(
							type = "text",
							attr = @DataTableColumnEditorAttr(key = "data-dt-field-hr", value = "after")
					)
			}
	)
	private String seconds = "0";

	@DataTableColumn(
			renderFormat = "dt-format-boolean-true",
			title = "[[#{components.cron_task.after_start}]]",
			editor = {
					@DataTableColumnEditor(
							type = "checkbox"
					)
			}
	)
	private boolean runAtStartup = false;

	@DataTableColumn(
			renderFormat = "dt-format-boolean-true",
			title = "[[#{components.cron_task.allow}]]",
			editor = {
					@DataTableColumnEditor(
							type = "checkbox"
					)
			}
	)
	private boolean enableTask = true;

	@DataTableColumn(
			renderFormat = "dt-format-boolean-true",
			title = "[[#{components.cron_task.audit}]]",
			editor = {
					@DataTableColumnEditor(
							type = "checkbox"
					)
			}
	)
	private boolean audit = true;
	private boolean businessDays = false;

	@DataTableColumn(inputType = DataTableColumnType.TEXT, title = "[[#{admin.crontab.cluster_node}]]", editor = {
		@DataTableColumnEditor(
			attr = {
				@DataTableColumnEditorAttr(key = "data-ac-url", value = "/admin/rest/settings/cronjob/nodes"),
				@DataTableColumnEditorAttr(key = "data-ac-min-length", value = "1"),
				@DataTableColumnEditorAttr(key = "data-ac-select", value = "true")
			}
		)
	})
	private String clusterNode = "all";

	public CronTask() {
		// empty constructor
	}

	public String receiveCronPattern() {
		String dayOfWeek = this.daysOfWeek;
		/* jeeff: businessDays uz nepouzivame (GUI nepodporuje)
		if ("*".equals(dayOfWeek) && businessDays)
			dayOfWeek = "1-5";
		*/

		StringBuilder cronPattern = new StringBuilder();
		cronPattern.append(seconds).append(' ').
				append(minutes).append(' ').
				append(hours).append(' ').
				append(daysOfMonth).append(' ').
				append(months).append(' ').
				append(dayOfWeek).append(' ').
				append(years);
		return cronPattern.toString();
	}

	public Class<?> receiveClazz() throws ClassNotFoundException {
		return Class.forName(task);
	}

	public String[] receiveArgs() {
		if (isEmpty(params))
			return new String[]{};
		return params.split("\\|");
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getSeconds() {
		return seconds;
	}

	public void setSeconds(String seconds) {
		this.seconds = seconds;
	}

	public String getMinutes() {
		return minutes;
	}

	public void setMinutes(String minutes) {
		this.minutes = minutes;
	}

	public String getHours() {
		return hours;
	}

	public void setHours(String hours) {
		this.hours = hours;
	}

	public String getMonths() {
		return months;
	}

	public void setMonths(String months) {
		this.months = months;
	}

	public String getYears() {
		return years;
	}

	public void setYears(String years) {
		this.years = years;
	}

	public String getDaysOfMonth() {
		return daysOfMonth;
	}

	public void setDaysOfMonth(String daysOfMonth) {
		this.daysOfMonth = daysOfMonth;
	}

	public String getDaysOfWeek() {
		return daysOfWeek;
	}

	public void setDaysOfWeek(String daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}

	public String getTask() {
		return task;
	}

	public void setTask(String task) {
		this.task = task;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params == null ? "" : params;
	}

	public boolean isBusinessDays() {
		return businessDays;
	}

	public void setBusinessDays(boolean businessDays) {
		this.businessDays = businessDays;
	}

	public String getClusterNode() {
		return clusterNode;
	}

	public void setClusterNode(String clusterNode) {
		this.clusterNode = clusterNode;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return String.format("%s that runs every %s, audit task: %b", getTask(), receiveCronPattern(), getAudit());
	}

	public void setAudit(boolean log) {
		this.audit = log;
	}

	public boolean getAudit() {
		return audit;
	}

	public boolean isRunAtStartup() {
		return runAtStartup;
	}

	public void setRunAtStartup(boolean runAtStartup) {
		this.runAtStartup = runAtStartup;
	}

	public boolean isEnableTask() {
		return enableTask;
	}

	public void setEnableTask(boolean enableTask) {
		this.enableTask = enableTask;
	}

}