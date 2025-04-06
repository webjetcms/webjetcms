package sk.iway.iwcm.components.calendar.jpa;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "calendar_types")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CALENDAR_CREATE)
public class CalendarTypesEntity implements Serializable {

    @Id
    @Column(name = "type_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_calendar_types")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "name")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="calendar.name"
    )
    @Size(max = 128)
    @NotBlank
    private String name;

    //calendar.schvalovatel
    @Column(name = "schvalovatel_id")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title="calendar.schvalovatel"
    )
    private Integer approverId;

    @Column(name = "domain_id")
    private Integer domainId;
}
