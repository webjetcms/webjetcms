package sk.iway.iwcm.users;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 *  SettingsBean.java - riadok tabulky user_settings
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 26.11.2008 15:26:00
 *@modified     $Date: 2008/12/11 08:34:21 $
 */
public class SettingsBean implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1914378986017022300L;
	private int userSettingsId;
	private int userId;
	private String skey;
	private String svalue1;
	private String svalue2;
	private String svalue3;
	private String svalue4;
	private int sint1;
	private int sint2;
	private int sint3;
	private int sint4;
	private Timestamp sdate;
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
	public String getSvalue1()
	{
		return svalue1;
	}
	public void setSvalue1(String svalue1)
	{
		this.svalue1 = svalue1;
	}
	public String getSvalue2()
	{
		return svalue2;
	}
	public void setSvalue2(String svalue2)
	{
		this.svalue2 = svalue2;
	}
	public String getSvalue3()
	{
		return svalue3;
	}
	public void setSvalue3(String svalue3)
	{
		this.svalue3 = svalue3;
	}
	public String getSvalue4()
	{
		return svalue4;
	}
	public void setSvalue4(String svalue4)
	{
		this.svalue4 = svalue4;
	}
	public int getSint1()
	{
		return sint1;
	}
	public void setSint1(int sint1)
	{
		this.sint1 = sint1;
	}
	public int getSint2()
	{
		return sint2;
	}
	public void setSint2(int sint2)
	{
		this.sint2 = sint2;
	}
	public int getSint3()
	{
		return sint3;
	}
	public void setSint3(int sint3)
	{
		this.sint3 = sint3;
	}
	public int getSint4()
	{
		return sint4;
	}
	public void setSint4(int sint4)
	{
		this.sint4 = sint4;
	}
	public Timestamp getSdate()
	{
		return sdate;
	}
	public void setSdate(Timestamp sdate)
	{
		this.sdate = sdate;
	}
	
	
}
