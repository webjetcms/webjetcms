package sk.iway.iwcm.components.appvyhladavanie;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.appvyhladavanie.VyhladavanieApp")
@WebjetAppStore(
    nameKey = "components.app-vyhladavanie.title",
    descKey = "components.app-vyhladavanie.desc",
    itemKey = "cmp_app-vyhladavanie",
    imagePath = "/components/app-vyhladavanie/menuicon.png",
    galleryImages = "/components/app-vyhladavanie/",
    componentPath = "/components/app-vyhladavanie/vyhladavanie.jsp")
@Getter
@Setter
public class VyhladavanieApp extends WebjetComponentAbstract {
    
    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.app-vyhladavanie_info"
    )
    private String explain;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-vyhladavanie.id"
    )
    private String customSearchId;
}
