package sk.iway.iwcm.components.sitebrowser;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.sitebrowser.SiteBrowserApp")
@WebjetAppStore(
    nameKey = "components.site_browser.title",
    descKey = "components.site_browser.desc",
    itemKey = "cmp_site_browser",
    imagePath = "/components/site_browser/editoricon.gif",
    galleryImages = "/components/site_browser/",
    componentPath = "/components/site_browser/site_browser.jsp"
)
@Getter
@Setter
public class SiteBrowserApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        title="components.sitemap.root_group",
        tab = "basic",
        sortAfter = "externalLink",
        className = "dt-tree-dir-simple",
        editor = {
            @DataTableColumnEditor(
                attr = {
                    @DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/files")
                }
            )
        }
    )
    private String rootDir;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.site_browser.target")
    private String target = "_blank";

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN,
        title = "components.site_browser.show_actual_dir",
        tab = "basic"
    )
    private Boolean showActualDir = true;
}