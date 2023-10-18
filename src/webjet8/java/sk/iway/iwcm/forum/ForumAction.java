package sk.iway.iwcm.forum;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;


/**
 *  Pripravenie formularu na pridanie prispevku
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Sobota, 2003, november 8
 *@modified     $Date: 2003/12/10 22:13:51 $
 */
public class ForumAction extends Action
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
		int parentId;
		int docId;
		int forumId = -1;
		if (session == null)
		{
			return (mapping.findForward("logon"));
		}

		try
		{
			parentId = Integer.parseInt(request.getParameter("parent"));
			docId = Integer.parseInt(request.getParameter("docid"));
			if (request.getParameter("forumid")!=null)
			{
				forumId = Integer.parseInt(request.getParameter("forumid"));
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			parentId = 0;
			docId = 0;
		}

		ForumForm forumForm = new ForumForm();
		forumForm.setParentId(parentId);
		forumForm.setDocId(docId);

		if (forumId>0)
		{
			Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
			if (user!=null && user.isAdmin())
			{
				ForumBean forumBean = ForumDB.getForumBean(request, forumId);
				forumForm.setAuthorFullName(forumBean.getAutorFullName());
				forumForm.setAuthorEmail(forumBean.getAutorEmail());
				forumForm.setSubject(forumBean.getSubject());
				forumForm.setQuestion(forumBean.getQuestion());
				forumForm.setForumId(forumId);
				forumForm.setSendNotif(forumBean.isSendNotif());
			}
		}
		else
		{
			if (parentId > 0)
			{
				ForumBean forumBean = ForumDB.getForumBean(request, parentId);
				if (forumBean != null && forumBean.getSubject().startsWith("Re:") == false && forumBean.getParentId() != -1)
				{
					forumForm.setSubject("Re: " + forumBean.getSubject());
				}
				else if (forumBean != null)
				{
					forumForm.setSubject(forumBean.getSubject());
				}
			}

			//nastav autora a email
			Cookie cookies[] = request.getCookies();
			int len = cookies.length;
			int i;
			for (i=0; i<len; i++)
			{
				if ("forumname".equals(cookies[i].getName()))
				{
					forumForm.setAuthorFullName(Tools.URLDecode(cookies[i].getValue()));
				}
				if ("forumemail".equals(cookies[i].getName()))
				{
					forumForm.setAuthorEmail(Tools.URLDecode(cookies[i].getValue()));
				}
			}
		}


		request.setAttribute("forumForm", forumForm);

		return (mapping.findForward("success"));
	}
}
