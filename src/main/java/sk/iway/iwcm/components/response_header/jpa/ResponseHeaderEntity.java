package sk.iway.iwcm.components.response_header.jpa;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "response_headers")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_RESPONSE_HEADER)
public class ResponseHeaderEntity {

    @Id
    @Column(name = "response_header_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_response_headers")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Column(name = "url")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="apps.response-header.url",
        ai = false
    )
    @NotEmpty
    @Size(max = 255)
    private String url;

    @Column(name = "header_name")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="apps.response-header.header_name"
    )
    @NotEmpty
    @Size(max = 255)
    private String headerName;

    @Column(name = "header_value")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA_WRAP,
        title="apps.response-header.header_value"
    )
    @NotEmpty
    private String headerValue;

    @Column(name = "change_date")
    //deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="apps.response-header.change_date",
        disabled = true
    )
    private Date changeDate;

    @Column(name = "note")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="apps.response-header.note"
    )
    @Size(max = 255)
    private String note;

    @Column(name = "domain_id")
    private Integer domainId;
}
