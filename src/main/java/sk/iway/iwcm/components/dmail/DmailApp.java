package sk.iway.iwcm.components.dmail;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditor;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumnEditorAttr;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;
import sk.iway.iwcm.users.UsersDB;

@WebjetComponent("sk.iway.iwcm.components.dmail.DmailApp")
@WebjetAppStore(
    nameKey = "components.dmail.title",
    descKey = "components.dmail.desc",
    itemKey = "cmp_dmail",
    imagePath = "/components/dmail/editoricon.png",
    galleryImages = "/components/dmail/",
    componentPath = "/components/dmail/subscribe.jsp,/components/dmail/subscribe-simple.jsp",
    customHtml = "/apps/dmail/admin/editor-component.html"
)
@DataTableTabs(tabs = {
    @DataTableTab(id = "basic", title = "components.universalComponentDialog.title", selected = true),
    @DataTableTab(id = "componentIframeWindowTabSent", title = "components.dmail.camp.send_emails", content = ""),
    // @DataTableTab(id = "componentIframeWindowTabUnsubscribed", title = "components.admin_unsubscribed_email.unsubscribed_email", content = ""),
    @DataTableTab(id = "componentIframeWindowTabLimits", title = "components.dmail.domainlimits.list", content = "")
})
@Getter
@Setter
public class DmailApp extends WebjetComponentAbstract {

    @DataTableColumn(
        inputType = DataTableColumnType.SELECT,
        tab = "basic",
        title = "calendar.type",
        editor = @DataTableColumnEditor(
            options = {
                @DataTableColumnEditorAttr(key = "components.user.reg_form", value = "subscribe"),
                @DataTableColumnEditorAttr(key = "components.dmail.subscribeSimple", value = "subscribe-simple"),
                //@DataTableColumnEditorAttr(key = "components.dmail.unsubscribeForm", value = "unsubscribe")
            }
        )
    )
    private String typeId;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title = "components.dmail.camp.sender_name"
    )
    private String senderName;

    @DataTableColumn(
        inputType = DataTableColumnType.TEXT,
        tab = "basic",
        title = "components.dmail.camp.sender_email"
    )
    private String senderEmail;

    @DataTableColumn(
        inputType = DataTableColumnType.JSON,
        tab = "basic",
        title = "components.dmail.email_docid",
        className = "dt-tree-page"
    )
    private Integer emailBodyId;

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabSent", title="&nbsp;")
    private String iframe  = "/components/dmail/admin_campaigns.jsp";

    // @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabUnsubscribed", title="&nbsp;")
    // private String iframe2  = "/components/dmail/admin_unsubscribed.jsp";

    @DataTableColumn(inputType = DataTableColumnType.IFRAME, tab = "componentIframeWindowTabLimits", title="&nbsp;")
    private String iframe3  = "/components/dmail/admin-domainlimits-list.jsp";

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        Identity user = UsersDB.getCurrentUser(request);
        if (user != null) {
            senderEmail = user.getEmail();
            senderName = user.getFullName();
        }
    }

}
