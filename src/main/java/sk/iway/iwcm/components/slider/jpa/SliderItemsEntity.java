
package sk.iway.iwcm.components.slider.jpa;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.system.adminlog.EntityListenersType;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@Entity
@Table(name = "slider")
@Getter
@Setter
@EntityListeners(sk.iway.iwcm.system.adminlog.AuditEntityListener.class)
@EntityListenersType(sk.iway.iwcm.Adminlog.TYPE_CALENDAR_CREATE)
public class SliderItemsEntity {

    public SliderItemsEntity() {
        // konstruktor
    }

    @Id
    private Long id;

    @Transient
    @DataTableColumn(inputType = DataTableColumnType.GALLERY_IMAGE, title="editor.perex.image", hiddenEditor = true)
    private String image;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.app-cookiebar.cookiebar_title",
        tab = "basic"
    )
    private String title;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXTAREA,
        title = "editor.subtitle",
        tab = "basic"
    )
    private String description;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        title = "components.news.redirectAfterClick",
        tab = "basic"
    )
    private String redirectUrl;

}
