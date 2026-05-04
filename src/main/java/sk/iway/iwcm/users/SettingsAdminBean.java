package sk.iway.iwcm.users;

import java.io.Serializable;

/**
 *  SettingsAdminBean.java - riadok tabulky user_settings_admin
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: kmarton $
 *@version      $Revision: 1.1 $
 *@created      Date: 02.07.2010 15:26:00
 *@modified     $Date: 2010/07/03 08:34:21 $
 */
public class SettingsAdminBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	private int userSettingsId;
	private int userId;
	private String skey;
	private String value;
	
	
	public SettingsAdminBean(int userId, String skey, String value)
	{
		this.userId = userId;
		this.skey = skey;
		this.value = value;
	}
	
	public SettingsAdminBean(int userSettingsId, int userId, String skey, String value)
	{
		this.userSettingsId = userSettingsId;
		this.userId = userId;
		this.skey = skey;
		this.value = value;
	}

	
	public int getUserSettingsId()
	{
		return userSettingsId;
	}
	public void setUserSettingsId(int userSettingsId)
	{
		this.userSettingsId = userSettingsId;
	}
	public int getUserId()
	{
		return userId;
	}
	public void setUserId(int userId)
	{
		this.userId = userId;
	}
	public String getSkey()
	{
		return skey;
	}
	public void setSkey(String skey)
	{
		this.skey = skey;
	}
	public String getValue()
	{
		return value;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
}