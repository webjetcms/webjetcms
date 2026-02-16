package sk.iway.iwcm.helpers;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.users.UsersDB;

/**
 *  RequestHelper.java
 *
 *		Encapsulates access to request and session-bound objects accumulated during filter and jsp execution
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 5.5.2010 15:12:00
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class RequestHelper
{

	HttpServletRequest request;

	public RequestHelper(HttpServletRequest request)
	{
		super();
		this.request = request;
	}

	public TemplateDetails getTemplate()
	{
		return (TemplateDetails)request.getAttribute("templateDetails");
	}

	public DocDetails getDocument()
	{
		return (DocDetails)request.getAttribute("docDetails");
	}

	public GroupDetails getGroup()
	{
		return (GroupDetails)request.getAttribute("pageGroupDetails");
	}

	public Identity getUser()
	{
		return UsersDB.getCurrentUser(request);
	}

}