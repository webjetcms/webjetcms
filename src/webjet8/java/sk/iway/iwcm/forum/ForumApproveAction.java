package sk.iway.iwcm.forum;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;


/**
 *  Ulozenie prispevku do fora
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Sobota, 2003, november 8
 *@modified     $Date: 2005/10/25 06:48:05 $
 */
public class ForumApproveAction extends Action
{
	/**
	 *  Description of the Method
	 *
	 *@param  mapping          Description of the Parameter
	 *@param  form             Description of the Parameter
	 *@param  request          Description of the Parameter
	 *@param  response         Description of the Parameter
	 *@return                  Description of the Return Value
	 *@exception IOException  Description of the Exception
	 */
	@Override
	public ActionForward execute(ActionMapping mapping,
			ActionForm form,
			HttpServletRequest request,
			HttpServletResponse response)
			 throws IOException
	{
		boolean updateOK = false;
		boolean deleted = false;
		int forumId = Tools.getIntValue(request.getParameter("forumId"), -1);
		final int DEL_CONST = 33;

		String hashCode = "";
		UserDetails user = null;
		String forumCode = request.getParameter("forumCode");
		ForumBean fb = ForumDB.getForumBean(request, forumId);
		if (fb != null)
		{
			hashCode = fb.getHashCode();
			if (fb.getUserId()>0) user = UsersDB.getUser(fb.getUserId());
			else
			{
				//aby sa dali mazat aj prispevky od neregistrovaneho usera
				user = new UserDetails();
				user.setUserId(-1);
				user.setLastName(fb.getAutorFullName());
				user.setEmail(fb.getAutorEmail());
				user.setAdmin(false);
			}
		}

		try
		{

			if(user != null)
			{
				if (forumId > 0 && forumCode != null && forumCode.equals(hashCode))
				{
					updateOK = ForumDB.forumApprove(forumId);
				}
				else
				{
					if (forumId > 0 && forumCode != null && forumCode.equals(hashCode + DEL_CONST) && fb != null)
					{
						Identity u = new Identity(user);
						updateOK = ForumDB.deleteMessage(forumId, fb.getDocId(), u);
						deleted = true;
					}
				}

				if (updateOK)
					response.sendRedirect("/components/forum/forum_approve.jsp?updateOK=true&deleted=" + deleted);
				else
					response.sendRedirect("/components/forum/forum_approve.jsp?updateError=true");
			}
			else
				response.sendRedirect("/components/forum/forum_approve.jsp?updateError=true");
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		return (null);
	}
}
