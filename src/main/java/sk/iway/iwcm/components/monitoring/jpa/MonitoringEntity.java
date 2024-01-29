package sk.iway.iwcm.components.monitoring.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

@Entity
@Table(name = "monitoring")
@Getter
@Setter
public class MonitoringEntity {

    public MonitoringEntity(){}

    @Id
	@GeneratedValue(strategy=GenerationType.IDENTITY, generator="S_monitoring")
	@Column(name = "monitoring_id")
	@DataTableColumn(inputType = DataTableColumnType.ID, renderFormat = "dt-format-selector")
	private Long id;

    @Column(name = "date_insert")
    @DataTableColumn(inputType = DataTableColumnType.DATE, renderFormat = "dt-format-date-time", title = "components.monitoring.date_insert")
    private Date dayDate;

    @Column(name = "node_name")
	@DataTableColumn(inputType = DataTableColumnType.TEXT, renderFormat = "dt-format-text", editor = {
        @DataTableColumnEditor(type = "text") })
    private String nodeName;

    @Column(name = "db_active")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Integer dbActive;

    @Column(name = "db_idle")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Integer dbIdle;

    @Column(name = "mem_free")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Long memFree;

    @Column(name = "mem_total")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Long memTotal;

    @Column(name = "cache")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Integer cache;

    @Column(name = "sessions")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Integer sessions;

    @Column(name = "cpu_usage")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Double cpuUsage;

    @Column(name = "process_usage")
    @DataTableColumn(inputType = DataTableColumnType.NUMBER, renderFormat = "dt-format-number")
    private Double processUsage;
}