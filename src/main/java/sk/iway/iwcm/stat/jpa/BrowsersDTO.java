package sk.iway.iwcm.stat.jpa;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class BrowsersDTO {

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_country.order"
    )
	private Integer order;

    //Hidden, used just for filter and for chart
    @DataTableColumn(
        inputType = DataTableColumnType.DATE,
        title="editor.date",
        visible = false,
        className = "not-export"
    )
	private Date dayDate;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_browser.browser"
    )
	private String browser;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title="stat_browser.platform"
    )
	private String platform;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="stat_country.visits"
    )
	private Integer visits;

    @DataTableColumn(
        inputType = DataTableColumnType.NUMBER,
        title="%",
        renderFormat = "dt-format-number--decimal"
    )
	private Double percentage;

    public String getBrowser() {
        return fixUnknown(browser);
    }

    public String getPlatform() {
        return fixUnknown(platform);
    }

    private String fixUnknown(String text) {
        if ("???".equals(text)) {
            Prop prop = Prop.getInstance();
            return prop.getText("calendar.neznamy");
        }
        return text;
    }
}
