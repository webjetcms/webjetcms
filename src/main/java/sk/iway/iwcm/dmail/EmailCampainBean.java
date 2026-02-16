package sk.iway.iwcm.dmail;

import java.util.Date;

/**
 *  EmailCampainBean.java - riadok tabulky emails_campain
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2007
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Date: 10.10.2007 12:38:40
 *@modified     $Date: 2007/10/17 07:34:59 $
 */
public class EmailCampainBean
{
	private int emailsCampainId;
	private String senderName;
	private String senderEmail;
	private String subject;
	private String url;
	private int createdByUserId;
	private Date createDate = null;
	private int countOfRecipients;
	private int countOfSentEmails;
	private Date lastSentDate = null;
	private String userGroups;
	
	private Date sendAt = null;
	private String attachments;
	private String ccEmail;
	private String bccEmail;
	private String replyTo;
	
	public int getEmailsCampainId()
	{
		return emailsCampainId;
	}
	public void setEmailsCampainId(int emailsCampainId)
	{
		this.emailsCampainId = emailsCampainId;
	}
	public String getSenderName()
	{
		return senderName;
	}
	public void setSenderName(String senderName)
	{
		this.senderName = senderName;
	}
	public String getSenderEmail()
	{
		return senderEmail;
	}
	public void setSenderEmail(String senderEmail)
	{
		this.senderEmail = senderEmail;
	}
	public String getSubject()
	{
		return subject;
	}
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
	public String getUrl()
	{
		return url;
	}
	public void setUrl(String url)
	{
		this.url = url;
	}
	public int getCreatedByUserId()
	{
		return createdByUserId;
	}
	public void setCreatedByUserId(int createdByUserId)
	{
		this.createdByUserId = createdByUserId;
	}
	public Date getCreateDate()
	{
		return createDate == null ? null : (Date) createDate.clone();
	}
	public void setCreateDate(Date createDate)
	{
		this.createDate = createDate == null ? null : (Date) createDate.clone();
	}
	public int getCountOfRecipients()
	{
		return countOfRecipients;
	}
	public void setCountOfRecipients(int countOfRecipients)
	{
		this.countOfRecipients = countOfRecipients;
	}
	public int getCountOfSentEmails()
	{
		return countOfSentEmails;
	}
	public void setCountOfSentEmails(int countOfSentEmails)
	{
		this.countOfSentEmails = countOfSentEmails;
	}
	public Date getLastSentDate()
	{
		return lastSentDate == null ? null : (Date) lastSentDate.clone();
	}
	public void setLastSentDate(Date lastSentDate)
	{
		this.lastSentDate = lastSentDate == null ? null : (Date) lastSentDate.clone();
	}
	public String getUserGroups()
	{
		return userGroups;
	}
	public void setUserGroups(String userGroups)
	{
		this.userGroups = userGroups;
	}
	public String getAttachments()
	{
		return attachments;
	}
	public void setAttachments(String attachments)
	{
		this.attachments = attachments;
	}
	public String getCcEmail()
	{
		return ccEmail;
	}
	public void setCcEmail(String ccEmail)
	{
		this.ccEmail = ccEmail;
	}
	public String getBccEmail()
	{
		return bccEmail;
	}
	public void setBccEmail(String bccEmail)
	{
		this.bccEmail = bccEmail;
	}
	public String getReplyTo()
	{
		return replyTo;
	}
	public void setReplyTo(String replyTo)
	{
		this.replyTo = replyTo;
	}
	public Date getSendAt()
	{
		return sendAt == null ? null : (Date) sendAt.clone();
	}
	public void setSendAt(Date sendAt)
	{
		this.sendAt = sendAt == null ? null : (Date) sendAt.clone();
	}
	
	
}
