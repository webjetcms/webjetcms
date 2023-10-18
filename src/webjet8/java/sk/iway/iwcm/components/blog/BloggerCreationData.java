package sk.iway.iwcm.components.blog;

import javax.servlet.http.HttpServletRequest;

/**
 *  BloggerCreationData.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.4.2011 12:41:19
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BloggerCreationData
{
	String name;
	String surname;
	String password;
	String email;
	String login;
	
	public BloggerCreationData(){}
	
	public BloggerCreationData(HttpServletRequest request)
	{
		name = request.getParameter("name");
		surname = request.getParameter("surname");
		password = request.getParameter("password");
		email = request.getParameter("email");
		login = request.getParameter("login");
	}
}