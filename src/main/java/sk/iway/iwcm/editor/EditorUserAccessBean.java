package sk.iway.iwcm.editor;

import sk.iway.iwcm.users.UserDetails;

/**
 *  EditorUserAccessBean.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: bhric $
 *@version      $Revision: 1.2 $
 *@created      Date: 27.10.2008 18:31:50
 *@modified     $Date: 2008/10/28 15:14:59 $
 */
public class EditorUserAccessBean
{
	private UserDetails user;
	private long datumPoslednejAktivity;
	
	public UserDetails getUser()
	{
		return user;
	}
	public void setUser(UserDetails user)
	{
		this.user = user;
	}
	public long getDatumPoslednejAktivity()
	{
		return datumPoslednejAktivity;
	}
	public void setDatumPoslednejAktivity(long datumPoslednejAktivity)
	{
		this.datumPoslednejAktivity = datumPoslednejAktivity;
	}
}
