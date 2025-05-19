package sk.iway.iwcm.stat;

import java.util.Date;
/**
 *  drzi data o last doc id
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1.1.1 $
 *@created      Piatok, 2002, máj 31
 *@modified     $Date: 2003/01/28 11:30:13 $
 */
public class SessionDetails
{
   private int lastDocId=-1;
   private String lastURL = "";
   private String remoteAddr;
   private long logonTime;
   private long lastActivity;
   private String loggedUserName = null;
   private int loggedUserId = -1;
   private boolean admin = false;
   private int domainId;
   private String domainName;

	public String getRemoteAddr()
	{
		return remoteAddr;
	}
	public void setRemoteAddr(String remoteAddr)
	{
		this.remoteAddr = remoteAddr;
	}
	public long getLastActivity()
	{
		return lastActivity;
	}
	public Date getLastActivityAsDate()
	{
		return new Date(lastActivity);
	}
	public void setLastActivity(long lastActivity)
	{
		this.lastActivity = lastActivity;
	}
	public long getLogonTime()
	{
		return logonTime;
	}
	public Date getLogonTimeAsDate()
	{
		return new Date(logonTime);
	}
	public void setLogonTime(long logonTime)
	{
		this.logonTime = logonTime;
	}
	public String getLastURL()
	{
		return lastURL;
	}
	public void setLastURL(String lastURL)
	{
		this.lastURL = lastURL;
	}
	public int getLastDocId()
	{
		return lastDocId;
	}
	public void setLastDocId(int lastDocId)
	{
		this.lastDocId = lastDocId;
	}
	public String getLoggedUserName()
	{
		return loggedUserName;
	}
	public void setLoggedUserName(String loggedUserName)
	{
		this.loggedUserName = loggedUserName;
	}
	public int getLoggedUserId()
	{
		return loggedUserId;
	}
	public void setLoggedUserId(int loggedUserId)
	{
		this.loggedUserId = loggedUserId;
	}
	public boolean isAdmin()
	{
		return admin;
	}
	public void setAdmin(boolean admin)
	{
		this.admin = admin;
	}
	public int getDomainId()
	{
		return domainId;
	}
	public void setDomainId(int domainId)
	{
		this.domainId = domainId;
	}
	public String getDomainName()
	{
		return domainName;
	}
	public void setDomainName(String domainName)
	{
		this.domainName = domainName;
	}

}
