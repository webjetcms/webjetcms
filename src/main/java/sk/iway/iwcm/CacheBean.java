package sk.iway.iwcm;

/**
 *  CacheBean.java - objekt, ktory sa nachadza v Cache (viz Cache.java)
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2004
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      $Date: 2008/05/30 07:23:33 $
 *@modified     $Date: 2008/05/30 07:23:33 $
 */
public class CacheBean
{
	private String name;
	private Object object;
	private long expiryTime;
	private boolean allowSmartRefresh = false;
	private boolean smartRefreshed = false;
	
	/**
	 * @return Returns the expiryTime.
	 */
	public long getExpiryTime()
	{
		return expiryTime;
	}
	/**
	 * @param expiryTime The expiryTime to set.
	 */
	public void setExpiryTime(long expiryTime)
	{
		this.expiryTime = expiryTime;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName()
	{
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	/**
	 * @return Returns the object.
	 */
	public Object getObject()
	{
		return object;
	}
	/**
	 * @param object The object to set.
	 */
	public void setObject(Object object)
	{
		this.object = object;
	}
	public boolean isAllowSmartRefresh()
	{
		return allowSmartRefresh;
	}
	public void setAllowSmartRefresh(boolean allowSmartRefresh)
	{
		this.allowSmartRefresh = allowSmartRefresh;
	}
	public boolean isSmartRefreshed()
	{
		return smartRefreshed;
	}
	public void setSmartRefreshed(boolean smartRefreshed)
	{
		this.smartRefreshed = smartRefreshed;
	}
}
