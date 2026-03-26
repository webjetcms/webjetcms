package sk.iway.iwcm.components.dmail;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.dmail.jpa.UnsubscribedEntity;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.users.UsersDB;

@WebjetComponent("sk.iway.iwcm.components.dmail.UnsubscribeApp")
@WebjetAppStore(
    nameKey = "components.dmail.unsubscribe.title",
    descKey = "components.dmail.unsubscribe.desc",
    itemKey = "cmp_dmail",
    variant = "unsubscribe",
    imagePath = "/components/dmail/editoricon.png",
    galleryImages = "/components/dmail/",
    componentPath = "/components/dmail/unsubscribe.jsp"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "unsubscribed", title = "components.admin_unsubscribed_email.unsubscribed_email")
})
@Getter
@Setter
public class UnsubscribeApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title = "components.dmail.camp.sender_email"
    )
    private String senderEmail;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title = "components.dmail.camp.sender_name"
    )
    private String senderName;

    @DataTableColumn(
        inputType = DataTableColumnType.BOOLEAN_TEXT,
        tab = "basic",
        title = "components.dmail.unsubscribe.confirmUnsubscribe"
    )
    private Boolean confirmUnsubscribe;

    @DataTableColumn(
        inputType = DataTableColumnType.QUILL,
        tab = "basic",
        title = "components.dmail.unsubscribe.confirmUnsubscribeText.title",
        editor = @DataTableColumnEditor(
            message = "components.dmail.unsubscribe.confirmUnsubscribeText"
        )
    )
    private String confirmUnsubscribeText;

    @DataTableColumn(
        inputType = DataTableColumnType.DATATABLE,
        tab = "unsubscribed",
        title = "&nbsp;",
        editor = { @DataTableColumnEditor(
            attr = {
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-url", value = "/admin/rest/dmail/unsubscribed"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-columns", value = "sk.iway.iwcm.dmail.jpa.UnsubscribedEntity"),
                @DataTableColumnEditorAttr(key = "data-dt-field-dt-serverSide", value = "true")
            }
        )}
    )
    private List<UnsubscribedEntity> unsubscribedEmails;

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user != null) {
            senderEmail = user.getEmail();
            senderName = user.getFullName();
        }
        if (confirmUnsubscribeText == null) {
            confirmUnsubscribeText = "<p>"+Prop.getInstance(request).getText("components.dmail.unsubscribe.confirmUnsubscribeText")+"</p>";
        }
        if (confirmUnsubscribe == null) {
            confirmUnsubscribe = true;
        }
    }

}
