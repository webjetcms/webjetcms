package sk.iway.iwcm.components.sendlink;
import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;

@WebjetComponent("sk.iway.iwcm.components.sendlink.SendLinkApp")
@WebjetAppStore(
    nameKey = "components.send_link.title",
    descKey = "components.send_link.desc",
    itemKey = "cmp_send_link",
    imagePath = "/components/send_link/editoricon.png",
    galleryImages = "/components/send_link/",
    componentPath = "/components/send_link/send_link.jsp")
@Getter
@Setter
public class SendLinkApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.RADIO,
        title = "&nbsp;",
        tab = "basic",
        editor = {
            @DataTableColumnEditor(
                options = {
                    @DataTableColumnEditorAttr(key = "components.send_link.send_page", value = "page"),
                    @DataTableColumnEditorAttr(key = "components.send_link.send_link", value = "link")
                }
            )
        }
    )
    private String sendType = "link";
}
