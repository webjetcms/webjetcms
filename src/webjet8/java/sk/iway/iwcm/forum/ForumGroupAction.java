package sk.iway.iwcm.forum;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.users.UsersDB;

/**
 *  ForumGroupAction.java
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 13.12.2004 14:47:50
 *@modified     $Date: 2007/09/07 13:39:30 $
 */
public class ForumGroupAction extends Action
{

	/**
	 *  Description of the Method
	 *
	 *@param  mapping          Description of the Parameter
	 *@param  form             Description of the Parameter
	 *@param  request          Description of the Parameter
	 *@param  response         Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception  IOException  Description of the Exception
	 */
	@Override
	public ActionForward execute(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws IOException
	{
		HttpSession session = request.getSession();
		Identity user = UsersDB.getCurrentUser(request);
		if (session == null || user == null || user.isAdmin()==false)
		{
			return (mapping.findForward("logon"));
		}

		ForumGroupBean fgb = (ForumGroupBean) form;
		Logger.println(this,"ForumGroupAction...");
		boolean saveOK = false;


		try
		{
			if (fgb != null)
			{
				saveOK = ForumDB.saveForum(fgb);
				if (saveOK)
				{
					//request.getRequestDispatcher("/components/forum/admin_forum_open.jsp?saveOK=true&docid="+fgb.getDocId()).forward(request, response);
					response.sendRedirect("/components/forum/admin_forum_open.jsp?saveOK=true&docid="+fgb.getDocId());
				}
				else
				{
					//request.getRequestDispatcher("/components/forum/admin_forum_open.jsp?saveError=true&docid="+fgb.getDocId()).forward(request, response);
					response.sendRedirect("/components/forum/admin_forum_open.jsp?saveError=true&docid="+fgb.getDocId());
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (null);
	}
}
