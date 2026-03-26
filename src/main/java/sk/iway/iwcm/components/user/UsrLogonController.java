package sk.iway.iwcm.components.user;

import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PathFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.components.WebjetComponentAbstract;
import sk.iway.iwcm.filebrowser.EditForm;
import sk.iway.iwcm.system.spring.SpringUrlMapping;
import sk.iway.iwcm.users.UsersDB;

/**
 * Logon on standard webpage to private section,
 * page should have included /components/user/logon.jsp
 */
@Controller
public class UsrLogonController extends WebjetComponentAbstract {

    @GetMapping("/usrlogon.struts")
    public String usrlogonShowForm(HttpServletRequest request) {
        return SpringUrlMapping.getCustomPath("/components/user/logon", request);
    }

    @PostMapping("/usrlogon.struts")
    @ResponseBody
    public void usrlogon(@RequestParam(required = false) String username, @RequestParam(required = false) String password, @RequestParam(required = false) Integer docId, HttpServletRequest request, HttpServletResponse response) {

        HttpSession session = request.getSession();
        String afterLogonRedirect = (String)session.getAttribute("afterLogonRedirect");
        int origDocId = Tools.getIntValue(request.getParameter("origDocId"), -1);
        if (origDocId>0) docId = origDocId;

        List<String> errors = LogonTools.logonUserWithAllChecks(request, username, password);
        try {
            Identity user = UsersDB.getCurrentUser(request);

            if (errors.isEmpty() && user != null) {

                PathFilter.setNginxProxyMode(request, response);

                //zavola triedu/metodu z konstanty. (robene kvoli plussport, kde sa namiesto session pouzila cookie)
		        LogonTools.afterLogon(user, request, response);


                if (afterLogonRedirect!=null)
                {
                    Logger.println(LogonTools.class, "session ID="+request.getSession().getId());

                    request.setAttribute("afterLogonRedirect", afterLogonRedirect);
                    Logger.println(LogonTools.class, "logon redirect url="+afterLogonRedirect);

                    Logger.println(this, "form="+docId);

                    if (afterLogonRedirect.startsWith("/files"))
                    {
                        //ak je to subor pre istotu znova skontroluj prava, aby sa to necyklilo
                        EditForm ef = PathFilter.isPasswordProtected(afterLogonRedirect, request);
                        if (ef != null && ef.isAccessibleFor(user)==false)
                        {
                            if (PathFilter.doFileForbiddenRedirect(ef, user, afterLogonRedirect, request, response)) return;
                        }
                    }

                    if (afterLogonRedirect.startsWith("http") && afterLogonRedirect.indexOf("/files/")==-1)
                    {
                        response.sendRedirect(afterLogonRedirect);
                        return;
                    }

                    request.getRequestDispatcher("/components/user/fileforward.jsp").forward(request, response);
                    return;
                }
                else
                {
                    request.setAttribute("docid", Integer.toString(docId));
                }
            }

            if (docId!=null && docId.intValue()>0) {

                StringBuilder url = new StringBuilder("/showdoc.do?docid=").append(docId.intValue());
                boolean useRedirect = false;

                if (errors.isEmpty() && user != null) {
                    String succesUrl = request.getParameter("successUrl");
                    if (request.getAttribute("successUrl")!=null){
                        succesUrl = request.getAttribute("successUrl").toString();
                    }

                    //security: allow only forwards to local addresses, if requested to external address use redirect set in webpage
                    if (Tools.isNotEmpty(succesUrl) && succesUrl.startsWith("/") && succesUrl.contains("//")==false)
                    {
                        url = new StringBuilder(succesUrl);
                        //we must redirect, because we are allready in Struts action and the mapping to docid will not work
                        useRedirect = true;
                    }

                    //skus najst redirect podla user skupiny
                    StringTokenizer st = new StringTokenizer(user.getUserGroupsIds(), ",");
                    int groupId;
                    String tmpRedir;
                    while (st.hasMoreTokens())
                    {
                        groupId = Tools.getIntValue(st.nextToken(), -1);
                        tmpRedir = request.getParameter("afterLogonRedirectGroup_"+groupId);
                        if (groupId > 0 && Tools.isNotEmpty(tmpRedir))
                        {
                            url = new StringBuilder(tmpRedir);
                            break;
                        }
                    }
                }

				Enumeration<String> parameters = request.getParameterNames();
				while (parameters.hasMoreElements())
				{
					String name = parameters.nextElement();
                    if ("docid".equals(name) || "docId".equals(name) || "username".equals(name) ||
                        "password".equals(name) || "doShowdocAction".equals(name) ||
                        "emailLogon".equals(name) || "successUrl".equals(name) ||
                        "origDocId".equals(name) || "tryNormalLogon".equals(name) ||
                        "newPassword".equals(name) || "retypeNewPassword".equals(name) ||
                        "org.apache.struts.taglib.html.TOKEN".equals(name)) continue;

					String values[] = request.getParameterValues(name);
                    for (int i=0; i<values.length; i++)
                    {
                        if (url.indexOf("?")==-1)
                        {
                            url.append('?').append(Tools.URLEncode(name)).append('=').append(Tools.URLEncode(values[i]));
                        }
                        else
                        {
                            url.append('&').append(Tools.URLEncode(name)).append('=').append(Tools.URLEncode(values[i]));
                        }
                    }
				}

                if (useRedirect) {
                    response.sendRedirect(url.toString());
                } else {
                    request.getRequestDispatcher(url.toString()).forward(request, response);
                }
                return;
            }

            String origPath = PathFilter.getOrigPath(request);
            if ("/usrlogon.do".equals(origPath)) {
                request.getRequestDispatcher(SpringUrlMapping.getCustomPath("/components/user/logon", request)+".jsp").forward(request, response);
                return;
            }
            response.sendRedirect(origPath);
        } catch (Exception ex) {
            Logger.error(getClass(), ex);
        }
    }

}
