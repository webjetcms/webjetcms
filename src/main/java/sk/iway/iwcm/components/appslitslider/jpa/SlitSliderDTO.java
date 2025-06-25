package sk.iway.iwcm.components.appslitslider.jpa;

import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Getter
@Setter

public class SlitSliderDTO {

    @Id
    @DataTableColumn(inputType = DataTableColumnType.ID, title = "ID")
    private Long id;

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "editor.perex.image",
        renderFormat = "dt-format-image"
    )
    private String image;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.app-cookiebar.cookiebar_title")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "editor.subtitle")
    private String subtitle;

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "components.news.redirectAfterClick")
	@javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    private String redirectUrl;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.app-slit_slider.admin_news_list.headingColor")
    private String headingColor;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.app-slit_slider.admin_news_list.fontColor")
    private String subheadingColor;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.app-slit_slider.admin_news_list.colors")
    private String backgroundColor;
}
