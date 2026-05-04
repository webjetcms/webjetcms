package sk.iway.iwcm.components.stat;

import jakarta.validation.constraints.NotBlank;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.stat.StatApp")
@WebjetAppStore(
    nameKey = "components.stat.title",
    descKey = "components.stat.desc",
    itemKey = "cmp_stat",
    imagePath = "/components/stat/editoricon.png",
    galleryImages = "/componenst/stat/",
    componentPath = "/components/stat/heat_map_tracker.jsp")
@Getter
@Setter
public class StatApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.stat.heat_map.warning"
    )
    private String explain;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.stat.heat_map.container"
    )
    @NotBlank
    private String container = "div.mainContainer";
}
