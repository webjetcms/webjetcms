package sk.iway.iwcm.helpers;

import static sk.iway.iwcm.Tools.isAnyEmpty;
import static sk.iway.iwcm.Tools.isEmpty;
import static sk.iway.iwcm.Tools.isNotEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.utils.Pair;

/**
 *  MailHelper
 *
 *  Facade over {@link SendMail} class
 */

@Getter
@Setter
@Accessors(chain = true)
public class MailHelper
{
	String fromName;
	String fromEmail;
	String toEmail = "";
	String ccEmail;
	String bccEmail;
	String subject;
	String message;
	String attachments = "";
	String replyTo;
	String baseHref;
	boolean sendLaterWhenException = true;
	boolean writeToAuditLog = true;
	List< Pair<String, String> > headers = null;

	public boolean send() {
		Boolean status = sendCapturingException().getFirst();
		if (status == null) return false;
		return status.booleanValue();
	}

	public Pair<Boolean, Exception> sendCapturingException()
	{
		if (isAnyEmpty(fromName, fromEmail, toEmail, message))
			throw new IllegalStateException("In order to send mail, these fields must be set: fromName, fromEmail, toEmail, message");

		return SendMail.sendCapturingException(this);
	}

	private String appendNewMailTo(String mail, String where)
	{
		if (isNotEmpty(where) && where.contains(mail))
			return where;
		where = isEmpty(where) ? "" : where + ", ";
		where += mail;
		return where;
	}

	public MailHelper addRecipient(String email)
	{
		toEmail = appendNewMailTo(email, toEmail);
		return this;
	}

	public MailHelper addCcRecipient(String email)
	{
		ccEmail = appendNewMailTo(email, ccEmail);
		return this;
	}

	public MailHelper addBccEmail(String email)
	{
		bccEmail = appendNewMailTo(email, bccEmail);
		return this;
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

	public MailHelper setBaseHref(HttpServletRequest request)
	{
		baseHref = Tools.getBaseHref(request)+"/";
		return this;
	}

	public MailHelper setBaseHref(String baseHref)
	{
		this.baseHref = baseHref;
		return this;
	}

	public MailHelper addHeader(String name, String value)
	{
		addHeader(new Pair<>(name, value));
		return this;
	}

	public MailHelper addHeader(Pair<String, String> header)
	{
		if (headers == null) headers = new ArrayList<>();
		headers.add(header);
		return this;
	}
}