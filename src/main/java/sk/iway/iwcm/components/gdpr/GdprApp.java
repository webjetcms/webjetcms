package sk.iway.iwcm.components.gdpr;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.gdpr.GdprApp")
@WebjetAppStore(
    nameKey = "components.gdpr.title",
    descKey = "components.gdpr.desc",
    itemKey = "cmp_gdpr",
    imagePath = "/components/gdpr/editoricon.png",
    galleryImages = "/components/gdpr/",
    componentPath = "/components/gdpr/cookie_bar.jsp")
@Getter
@Setter
public class GdprApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        title = "components.gdpr.cookies.showLink",
        tab = "basic"
    )
    private Boolean showLink;
}