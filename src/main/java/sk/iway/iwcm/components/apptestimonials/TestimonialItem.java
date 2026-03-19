package sk.iway.iwcm.components.apptestimonials;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter
public class TestimonialItem {

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        className = "image",
        title = "components.app-testimonials.selectImage",
        renderFormat = "dt-format-image-notext"
    )
    private String image;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, className="dt-row-edit", title = "editor.app-testimonials.name")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "editor.app-testimonials.text")
    private String description;

    @DataTableColumn(inputType = DataTableColumnType.BOOLEAN_TEXT, title = "components.news.redirectAfterClick")
    private Boolean doRedirectUrl;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "&nbsp;")
    private String redirectUrl;
}