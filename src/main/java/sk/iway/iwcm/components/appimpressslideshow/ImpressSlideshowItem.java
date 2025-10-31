package sk.iway.iwcm.components.appimpressslideshow;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Getter
@Setter
public class ImpressSlideshowItem {

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "editor.perex.image",
        renderFormat = "dt-format-image-notext"
    )
    private String image;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, className="dt-row-edit", title = "components.app-cookiebar.cookiebar_title")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.QUILL, title = "editor.subtitle")
    private String subtitle;

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "components.news.redirectAfterClick")
	@jakarta.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    private String redirectUrl;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.app-slit_slider.admin_news_list.headingColor")
    private String headingColor;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.app-slit_slider.admin_news_list.fontColor")
    private String subheadingColor;

    @DataTableColumn(inputType = DataTableColumnType.COLOR, title = "components.app-slit_slider.admin_news_list.colors")
    private String backgroundColor;

    //We dont use fields customStyleHeading and customStyleSubHeading BUT historic version of JSP need them, even when blank
    //Do not remove
    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String customStyleHeading;

    @DataTableColumn(inputType = DataTableColumnType.HIDDEN)
    private String customStyleSubHeading;
}