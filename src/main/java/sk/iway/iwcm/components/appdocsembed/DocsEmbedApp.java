package sk.iway.iwcm.components.appdocsembed;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.appdocsembed.DocsEmbedApp")
@WebjetAppStore(
    nameKey = "components.app-docsembed.title",
    descKey = "components.app-docsembed.desc",
    itemKey = "cmp_app-docsembed",
    imagePath = "/components/app-docsembed/editoricon.png",
    galleryImages = "/components/app-docsembed/",
    componentPath = "/components/app-docsembed/embed.jsp")
@Getter
@Setter
public class DocsEmbedApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        tab = "basic",
        title="components.app-docsembed.editor_components.url",
        className = "dt-style-base64"
    )
    private String url;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-docsembed.editor_components.height",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "900")
            }
        )
    })
    private String height = "900";

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title="components.app-docsembed.editor_components.width",
        editor = {
        @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "placeholder", value = "100%")
            }
        )
    })
    private String width = "100%";
}
