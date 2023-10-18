package sk.iway.iwcm.helpers;

import static sk.iway.iwcm.Tools.isAnyEmpty;
import static sk.iway.iwcm.Tools.isEmpty;
import static sk.iway.iwcm.Tools.isNotEmpty;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  MailHelper
 *  
 *  Facade over {@link SendMail} class
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 3.5.2010 13:29:00
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class MailHelper
{
	String fromName;
	String fromEmail;
	String toEmail = "";
	String ccEmail;
	String bccEmail;
	String subject;
	String text;
	String attachments = "";
	String replyTo;
	String baseHref;
	

	public boolean send()
	{
		if (isAnyEmpty(fromName, fromEmail, toEmail, text))
			throw new IllegalStateException("In order to send mail, these fields must be set: fromName, fromEmail, toEmail, text");
			
		return SendMail.send(fromName, fromEmail, toEmail, replyTo, ccEmail, bccEmail, subject, text, baseHref, attachments);
	}
	
	private String appendNewMailTo(String mail, String where)
	{
		if (isNotEmpty(where) && where.contains(mail))
			return where;
		where = isEmpty(where) ? "" : where + ", ";
		where += mail;
		return where;
	}
	
	
	public String getFromName()
	{
		return fromName;
	}
	public MailHelper setFromName(String fromName)
	{
		this.fromName = fromName;
		return this;
	}
	public String getFromEmail()
	{
		return fromEmail;
	}
	public MailHelper setFromEmail(String fromEmail)
	{
		this.fromEmail = fromEmail;
		return this;
	}
	public String getRecipients()
	{
		return toEmail;
	}
	public MailHelper setRecipients(String toEmail)
	{
		this.toEmail = toEmail;
		return this;
	}
	public MailHelper addRecipient(String email)
	{
		toEmail = appendNewMailTo(email, toEmail);
		return this;
	}
	
	public String getCcEmail()
	{
		return ccEmail;
	}
	public MailHelper addCcRecipient(String email)
	{
		ccEmail = appendNewMailTo(email, ccEmail);
		return this;
	}
	public String getBccEmail()
	{
		return bccEmail;
	}
	public MailHelper addBccEmail(String email)
	{
		bccEmail = appendNewMailTo(email, bccEmail);
		return this;
	}
	public String getSubject()
	{
		return subject;
	}
	public MailHelper setSubject(String subject)
	{
		this.subject = subject;
		return this;
	}
	public String getMessage()
	{
		return text;
	}
	public MailHelper setMessage(String text)
	{
		this.text = text;
		return this;
	}
	public String getAttachments()
	{
		return attachments;
	}
	
	public MailHelper addAttachment(IwcmFile file)
	{
		appendAttachment(file.getAbsolutePath(), file.getName());
		return this;
	}
	
	public MailHelper addAttachment(File file)
	{
		appendAttachment(file.getAbsolutePath(), file.getName());
		return this;
	}
	
	public MailHelper addNamedAttachment(File file, String displayName)
	{
		appendAttachment(file.getAbsolutePath(), displayName);
		return this;
	}
	
	public MailHelper addNamedAttachment(IwcmFile file, String displayName)
	{
		appendAttachment(file.getAbsolutePath(), displayName);
		return this;
	}
	
	private void appendAttachment(String path, String displayName)
	{
		if (isNotEmpty(attachments))
			attachments += ';';
		attachments += path + ';' + displayName;
	}
	
	public MailHelper removeAttachment(IwcmFile file)
	{
		attachments = attachments.replace(file.getAbsolutePath(), "");
		return this;
	}
	
	public MailHelper removeAttachment(File file)
	{
		attachments = attachments.replace(file.getAbsolutePath(), ">MARK<");
		attachments = attachments.replaceAll(">MARK<;.*(;|$)", "");
		return this;
	}
	
	public String getReplyTo()
	{
		return replyTo;
	}

	public MailHelper setReplyTo(String replyTo)
	{
		this.replyTo = replyTo;
		return this;
	}

	public String getBaseHref()
	{
		return baseHref;
	}

	public MailHelper setBaseHref(String baseHref)
	{
		this.baseHref = baseHref;
		return this;
	}
	
	public MailHelper setBaseHref(HttpServletRequest request)
	{
		baseHref = Tools.getBaseHref(request)+"/";
		return this;
	}

}