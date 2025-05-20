package sk.iway.iwcm.tags.support_logic.action;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, ServletRequest request, ServletResponse response) throws Exception {
        try {
            return execute(mapping, form, (HttpServletRequest) request,
                (HttpServletResponse) response);
        } catch (ClassCastException e) {
            return null;
        }
    }

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return null;
    }
}