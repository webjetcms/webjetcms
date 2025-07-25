package sk.iway.iwcm.components.carouselslider;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.jpa.AllowHtmlAttributeConverter;

@Getter
@Setter
public class CarouselSliderItem {

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "editor.perex.image",
        renderFormat = "dt-format-image-notext"
    )
    private String image;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, className="dt-row-edit", title = "components.app-cookiebar.cookiebar_title")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "editor.subtitle")
    private String description;

    @DataTableColumn(inputType = DataTableColumnType.ELFINDER, title = "components.news.redirectAfterClick")
    @javax.persistence.Convert(converter = AllowHtmlAttributeConverter.class)
    private String redirectUrl;
}
