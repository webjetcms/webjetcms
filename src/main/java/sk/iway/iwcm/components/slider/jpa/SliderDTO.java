package sk.iway.iwcm.components.slider.jpa;

import javax.persistence.Column;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Getter
@Setter

public class SliderDTO {

    @Id
    @DataTableColumn(inputType = DataTableColumnType.ID, title = "ID")
    private Long id;

    @DataTableColumn(inputType = DataTableColumnType.GALLERY_IMAGE, title = "editor.perex.image")
    private String image;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.app-cookiebar.cookiebar_title")
    private String title;

    @DataTableColumn(inputType = DataTableColumnType.TEXTAREA, title = "editor.subtitle")
    private String description;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.news.redirectAfterClick")
    private String redirectUrl;
}
