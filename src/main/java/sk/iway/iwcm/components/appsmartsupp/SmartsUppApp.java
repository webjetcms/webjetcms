package sk.iway.iwcm.components.appsmartsupp;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;

@WebjetComponent("sk.iway.iwcm.components.appsmartsupp.SmartsUppApp")
@WebjetAppStore(
    nameKey = "components.app-smartsupp.title",
    descKey = "components.app-smartsupp.desc",
    itemKey = "cmp_smartsupp",
    imagePath = "/components/app-smartsupp/editoricon.png",
    galleryImages = "/components/app-smartsupp/",
    componentPath = "/components/app-smartsupp/chat.jsp")
@Getter
@Setter
public class SmartsUppApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.STATIC_TEXT,
        tab = "basic",
        title="components.app-smartsupp.info"
    )
    private String explain;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-smartsupp.kluc"
    )
    private String kluc;
}