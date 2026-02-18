package sk.iway.iwcm.components.calendar.jpa;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnNested;

@MappedSuperclass
public class CalendarEventsBasic implements Serializable {

    @Id
    @Column(name = "calendar_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "S_calendar")
    @DataTableColumn(inputType = DataTableColumnType.ID)
    private Long id;

    @Lob
    @Column(name = "title")
    @DataTableColumn(
        inputType = DataTableColumnType.OPEN_EDITOR,
        title="calendar.name",
        tab = "basic"
    )
    @NotBlank
    private String title;

    @Column(name = "date_from")
	//deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="calendar.begin",
        tab = "basic"
    )
    @NotNull
	private Date dateFrom;

    @Column(name = "date_to")
	//deprecated, not need anymore @Temporal(TemporalType.TIMESTAMP)
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="calendar.end",
        tab = "basic"
    )
    @NotNull
	private Date dateTo;

    @Column(name = "area")
    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="calendar_edit.area",
        hidden = true,
        tab="advanced"
    )
    @Size(max = 255)
    private String area;

    @Column(name = "type_id")
    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        title = "calendar.type",
        tab = "advanced"
    )
    @NotNull
    private Integer typeId;

    @Column(name = "suggest")
    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "calendar.suggested",
        visible = false,
        hiddenEditor = true
    )
    private Boolean suggest;

    @Column(name = "creator_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    @NotNull
    private Integer creatorId;

    @Column(name = "domain_id")
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    @NotNull
    private Integer domainId;

    @Transient
    @DataTableColumnNested
	private CalendarEventsEditorFields editorFields = null;

    @Column(name = "approve")
    private Integer approve;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public Integer getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Integer creatorId) {
        this.creatorId = creatorId;
    }

    public Integer getDomainId() {
        return domainId;
    }

    public void setDomainId(Integer domainId) {
        this.domainId = domainId;
    }

    public CalendarEventsEditorFields getEditorFields() {
        return editorFields;
    }

    public void setEditorFields(CalendarEventsEditorFields editorFields) {
        this.editorFields = editorFields;
    }

    public Integer getApprove() {
        return approve;
    }

    public void setApprove(Integer approve) {
        this.approve = approve;
    }

    public Boolean getSuggest() {
        return suggest;
    }

    public void setSuggest(Boolean suggest) {
        this.suggest = suggest;
    }


}
