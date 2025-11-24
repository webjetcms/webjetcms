package org.apache.struts.action;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import sk.iway.iwcm.Logger;

/**
 * This class is deprecated, it's there only for users who didn't read documentation
 * and didn't remove mapping in web.xml
 */
@Deprecated
public class ActionServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        Logger.error(ActionServlet.class, "WARNING: ActionServlet is deprecated, please remove mapping for org.apache.struts.action.ActionServlet and *.do in web.xml");
    }

}
