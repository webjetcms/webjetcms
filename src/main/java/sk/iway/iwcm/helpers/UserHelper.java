package sk.iway.iwcm.helpers;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.users.UserDetails;

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
public class UserHelper
{

	HttpServletRequest request;

	public UserHelper(HttpServletRequest request)
	{
		super();
		this.request = request;
	}
	
	public Optional<UserDetails> getUser()
	{
		if (request.getSession() == null) {
			return Optional.ofNullable(null);
		}

		return Optional.ofNullable((UserDetails) request.getSession().getAttribute(Constants.USER_KEY));
	}

	public boolean isAdmin() {
		if (!getUser().isPresent()) {
			return false;
		}

		return getUser().get().isAdmin();
	}

	public boolean hasUserGroup(String name) {
		if (!getUser().isPresent()) {
			return false;
		}

		return getUser().get().isInUserGroup(name);
	}

	public boolean hasUserGroup(int id) {
		if (!getUser().isPresent()) {
			return false;
		}

		return getUser().get().isInUserGroup(id);
	}
}