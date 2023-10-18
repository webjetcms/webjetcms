package sk.iway.iwcm.components.form;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.ui.ModelMap;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.annotations.DefaultHandler;
import sk.iway.iwcm.system.annotations.WebjetComponent;


@WebjetComponent("sk.iway.iwcm.components.form.DoubleOptInComponent")
public class DoubleOptInComponent extends WebjetComponentAbstract {

    public enum Status {
        SUCCESS,
        ALREADY_CONFIRMED,
        FAIL
    }

    private final String VIEW = "/components/form/double_opt_in";

    @SuppressWarnings("unchecked")
    @DefaultHandler
    public String render(HttpServletRequest request, ModelMap model, int formId, String hash) {

        Status status = Status.FAIL;
        if (formId > 0 && Tools.isNotEmpty(hash)) {
            SimpleQuery simpleQuery = new SimpleQuery();
            List<String> list = simpleQuery.forList("SELECT * FROM forms WHERE id = ? AND double_optin_hash = ?", formId, hash);

            if (list.size() == 1) {
                list = simpleQuery.forList("SELECT * FROM forms WHERE id = ? AND double_optin_hash = ? AND double_optin_confirmation_date IS NOT NULL", formId, hash);
                if (list.size() > 0) {
                    status = Status.ALREADY_CONFIRMED;
                }
                else {
                    simpleQuery.execute("UPDATE forms SET double_optin_confirmation_date = ? WHERE id = ? AND double_optin_hash = ?", new Timestamp(new Date().getTime()), formId, hash);
                    status = Status.SUCCESS;
                    Adminlog.add(Adminlog.TYPE_FORMMAIL, Tools.getUserId(request), String.format("Double opt in success, formId: %d, hash: %s", formId, hash), formId, 0, new Timestamp(new Date().getTime()));
                }
            }
        }

        Prop prop = Prop.getInstance(request);
        model.addAttribute("status", status);
        model.addAttribute("text", prop.getText("doubleoptin.confirm_text." + status.name().toLowerCase()));
        model.addAttribute("title", prop.getText("doubleoptin.confirm_title." + status.name().toLowerCase()));

        return VIEW;
    }

    @Override
    public String getViewFolder() {
        return null;
    }
}
