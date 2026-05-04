package sk.iway.iwcm.dmail;

import java.util.Date;

/**
 *  EmailUnsubscribedBean.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2008
 *@author       $Author: pbezak $
 *@version      $Revision: 1.1 $
 *@created      Date: 29.1.2008 11:22:15
 *@modified     $Date: 2008/01/31 16:41:10 $
 */
public class EmailUnsubscribedBean
{
	private int emailsUnsubscribedId;
	private String email;
	private Date createDate;
	
	public String getEmail()
	{
		return email;
	}
	public void setEmail(String email)
	{
		this.email = email;
	}
	public int getEmailsUnsubscribedId()
	{
		return emailsUnsubscribedId;
	}
	public void setEmailsUnsubscribedId(int emailsUnsubscribedId)
	{
		this.emailsUnsubscribedId = emailsUnsubscribedId;
	}
	public Date getCreateDate()
	{
		return createDate;
	}
	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate;
	}
}
