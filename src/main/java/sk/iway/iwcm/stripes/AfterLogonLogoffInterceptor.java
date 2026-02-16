package sk.iway.iwcm.stripes;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.users.UserDetails;

/**
 *  AfterRegUserSaveInterceptor.java
 *
 *  Interface pre interceptor afterSave v triede sk.iway.iwcm.stripes.RegUserAction
 *
 *  FQN konkretnej implementacie sa zadava:
 *  <ul>
 *  <li>Globalne v konfiguracii do premennej <b>stripesUserAfterSaveClass</b></li>
 *  <li>Lokalne pre konkretnu stranku editformu v include PageParams ako premenna <b>afterSaveInterceptor</b></li>
 *  </ul>
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author       $Author: mbocko
 *@version      $Revision: 1.8 $
 *@created      Date: 16.10.2007 16:28:45
 */
public interface AfterLogonLogoffInterceptor
{
	public boolean logon(UserDetails user, HttpServletRequest request);
	
	public boolean logoff(UserDetails user, HttpServletRequest request);
}
