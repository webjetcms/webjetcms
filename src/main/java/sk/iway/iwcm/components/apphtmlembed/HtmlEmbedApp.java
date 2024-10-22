package sk.iway.iwcm.components.apphtmlembed;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;

@WebjetComponent("sk.iway.iwcm.components.apphtmlembed.HtmlEmbedApp")
@WebjetAppStore(
    nameKey = "components.app-htmlembed.title",
    descKey = "components.app-htmlembed.desc",
    itemKey = "cmp_app-htmlembed",
    imagePath = "/components/app-htmlembed/editoricon.png",
    galleryImages = "/components/app-htmlembed/",
    componentPath = "/components/app-htmlembed/embed.jsp")
@Getter
@Setter
public class HtmlEmbedApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.BASE64,
        tab = "basic",
        title="components.app-htmlembed.editor_components.vloz_html_kod",
        className = "textarea-code-small",
        editor = {
            @DataTableColumnEditor(message = "components.app-htmlembed.desc")
        }
    )
    private String html;
}
