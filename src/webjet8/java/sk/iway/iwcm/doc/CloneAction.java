package sk.iway.iwcm.doc;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.i18n.Prop;

/**
 *  CloneAction.java - naklonovanie adresarovej struktury pre novu jazykovu mutaciu
 *
 *@Title        webjet
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Date: 31.8.2004 14:55:35
 *@modified     $Date: 2007/09/07 13:39:29 $
 */
public class CloneAction extends Action
{
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException
	{
		HttpSession session = request.getSession();
		if (session == null)
		{
			return (mapping.findForward("logon_admin"));
		}
		Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
		if (user != null && user.isAdmin())
		{
		}
		else
		{
			return (mapping.findForward("logon_admin"));
		}
		Prop prop = Prop.getInstance(servlet.getServletContext(), request);
		
		response.setContentType("text/html; charset=windows-1250");

		request.setAttribute("iconLink", "");
		request.setAttribute("dialogTitle", prop.getText("admin.clone.dialogTitle"));
		request.setAttribute("dialogDesc", prop.getText("admin.clone.dialogDesc")+ ".");
		//	ukonci tabulku, aby bolo vidno ako sa to kopiruje
		//request.setAttribute("closeTable", "true");
		request.getRequestDispatcher("/admin/layout_top_dialog.jsp").include(request, response);
		
		Logger.println(this,"src="+request.getParameter("srcGroupId"));
		int srcGroupId = Tools.getIntValue(request.getParameter("srcGroupId"), -1);
		int destGroupId = Tools.getIntValue(request.getParameter("destGroupId"), -1);

		if(!GroupsDB.isGroupEditable(user,srcGroupId) || !GroupsDB.isGroupEditable(user,destGroupId))
		{
			request.setAttribute("err_msg", prop.getText("admin.editor_dir.dontHavePermsForThisDir"));
			return mapping.findForward("error_admin");
		}
		
		PrintWriter out = response.getWriter();
		out.println("<div class=\"padding10\">");
		//out.println("<html><head><LINK rel='stylesheet' href='/admin/css/style.css'><META http-equiv='Content-Type' content='text/html; charset=windows-1250'></head><body>");

		if (srcGroupId > 0 && destGroupId > 0)
		{
			String srcTempLang = request.getParameter("srcTempLang");
			String destTempLang = request.getParameter("destTempLang");
			CloneStructure cloner = new CloneStructure(out, mapping, request, response, servlet, srcTempLang, destTempLang);
			if ("clone".equals(request.getParameter("act")))
			{
				out.println("<h3>"+prop.getText("components.clone.cloning_please_wait")+"</h3>");
			   cloner.copyStructure(srcGroupId, destGroupId);
			   out.println("<hr>"+prop.getText("components.clone.done"));
			   out.println("<script type='text/javascript'>function Ok() {if(typeof window.opener.reloadWebpagesTree === \"function\") {window.opener.reloadWebpagesTree();} window.close();} </script>");
			}
		}
		out.print("</div>");
		request.getRequestDispatcher("/admin/layout_bottom_dialog.jsp").include(request, response);

		//out.println("</body></html>");

		return(null);
	}


}
