package org.apache.struts.action;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;

import sk.iway.iwcm.Logger;

/**
 * This class is deprecated, it's there only for users who didn't read documentation
 * and didn't remove mapping in web.xml
 * @deprecated ActionServlet is no longer used in Webjet, please remove mapping for org.apache.struts.action.ActionServlet and *.do in web.xml
 */
@Deprecated(forRemoval = true)
public class ActionServlet extends HttpServlet {

    @Override
    @Deprecated(forRemoval = true)
    public void init() throws ServletException {
        Logger.error(ActionServlet.class, "WARNING: ActionServlet is deprecated, please remove mapping for org.apache.struts.action.ActionServlet and *.do in web.xml");
    }

}
