package sk.iway.iwcm.components.seo.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class BotsDTO {

    public BotsDTO() {}

    public BotsDTO(Date dayDate, Integer visits) {
        this.dayDate = dayDate;
        this.visits = visits;
    }

    public BotsDTO(Integer botId, String name, Integer visits, Date dayDate) {
        this.botId = botId;
        this.name = name;
        this.visits = visits;
        this.dayDate = dayDate;
    }

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="ID"
    )
	private Integer botId;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_referer.server_name",
        renderFormatLinkTemplate = "javascript:getBotDetials({{botId}});"
    )
	private String name;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="components.seo.connects"
    )
	private Integer visits;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="%",
        renderFormat = "dt-format-number--decimal"
    )
	private Float percentual;

    //Aka last visit
	@DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="components.seo.last_visit"
    )
	private Date dayDate;
} 
