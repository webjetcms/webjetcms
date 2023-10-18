package sk.iway.iwcm.components.seo.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class BotsDetailsDTO {

    public BotsDetailsDTO() {}

    public BotsDetailsDTO(Date dayDate, Integer viewCount) {
        this.dayDate = dayDate;
        this.viewCount = viewCount;
    }

    public BotsDetailsDTO(Integer session, Date dayDate, String document, String lastDocument, String group) {
        this.session = session;
        this.dayDate = dayDate;
        this.document = document;
        this.lastDocument = lastDocument;
        this.group = group;
    }
    
    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.stat.user.viewstats.session"
    )
	private Integer session;

    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.stat.user.viewstats.viewtime"
    )
	private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.stat.user.viewstats.document"
    )
	private String document;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.stat.user.viewstats.lastdocument"
    )
	private String lastDocument;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="components.stat.user.viewstats.group"
    )
	private String group;

    private Integer viewCount;
}
