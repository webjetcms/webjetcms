package sk.iway.iwcm.components.form;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.editor.rest.ComponentRequest;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.annotations.WebjetAppStore;
import sk.iway.iwcm.system.annotations.WebjetComponent;
import sk.iway.iwcm.system.datatable.DataTableColumnType;
import sk.iway.iwcm.system.datatable.annotations.DataTableColumn;
import sk.iway.iwcm.system.datatable.annotations.DataTableTab;
import sk.iway.iwcm.system.datatable.annotations.DataTableTabs;

@WebjetComponent("sk.iway.iwcm.components.form.Captcha")
@WebjetAppStore(
    nameKey = "editor.form.captcha",
    descKey = "&nbsp;",
    imagePath = "ti ti-shield-lock",
    componentPath = "/components/form/captcha.jsp",
    customHtml = "/apps/form/admin/captcha.html",
    dontShow = true
)
@Getter
@Setter
@DataTableTabs(tabs = {@DataTableTab(id = "basic", title = "basic", selected = true)})
public class Captcha extends WebjetComponentAbstract  {

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.captcha.site_key", tab = "basic")
    String siteKey;

    @DataTableColumn(inputType = DataTableColumnType.TEXT, title = "components.captcha.secret", tab = "basic")
    String secret;

    @Override
    public void initAppEditor(ComponentRequest componentRequest, HttpServletRequest request) {
        ConfDetails siteKeyConfDetails = ConfDB.getVariable("reCaptchaSiteKey");
        this.siteKey = siteKeyConfDetails == null ? "" : siteKeyConfDetails.getValue();

        ConfDetails secretConfDetails = ConfDB.getVariable("reCaptchaSecret");
        this.secret = secretConfDetails == null ? "" : secretConfDetails.getValue();
    }
}