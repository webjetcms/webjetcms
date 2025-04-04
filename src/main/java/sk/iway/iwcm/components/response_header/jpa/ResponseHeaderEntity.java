package sk.iway.iwcm.components.response_header.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

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
        title="apps.response-header.url"
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
        inputType = DataTableColumnType.TEXT,
        title="apps.response-header.header_value"
    )
    @NotEmpty
    @Size(max = 255)
    private String headerValue;

    @Column(name = "change_date")
    @Temporal(TemporalType.TIMESTAMP)
    @DataTableColumn(
        inputType = DataTableColumnType.DATETIME,
        title="apps.response-header.change_date",
        hiddenEditor = true
    )
    private Date changeDate;

    @Column(name = "note")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title="apps.response-header.note"
    )
    @Size(max = 255)
    private String note;

    @Column(name = "domain_id")
    private Integer domainId;
}
