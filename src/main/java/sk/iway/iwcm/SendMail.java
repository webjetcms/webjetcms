package sk.iway.iwcm;

import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.helpers.MailHelper;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.io.IwcmFsDB;
import sk.iway.iwcm.system.context.ContextFilter;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.utils.Pair;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.activation.URLDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.http.HttpServletRequest;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.io.*;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;

//import sk.iway.intranet.dms.DmsNotify;

/**
 *  Odosielanie emailu, priklad telnet spojenia:

EHLO tau19.iway.sk
MAIL FROM:noreply@interway.sk
RCPT TO:veronika.husarova@employment.gov.sk
DATA
From:noreply@interway.sk
To:veronika.husarova@employment.gov.sk
Subject:test

Test
.
QUIT

 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.13 $
 *@created      Nedeďż˝e, 2003, februďż˝r 2
 *@modified     $Date: 2004/03/23 19:23:05 $
 */
public class SendMail
{

	/**
     * Comment for <code>AUTOREPLY_DAEMON</code>
     */
    	public static final String AUTOREPLY_SUBJECT = "AUTOREPLY-";
	public static final String EMAIL_PROTECTION_SENDER_KEY = "emailProtectionSenderEmail";
	public static final String EMAIL_PROTECTION_SENDER_NAME_KEY = "emailProtectionSenderName";

	public static boolean send(String fromName, String fromEmail, String toEmail, String subject, String text)
	{
		return (send(fromName, fromEmail, toEmail, null, null, subject, text, null));
	}

	public static boolean send(String fromName, String fromEmail, String toEmail, String subject, String text, HttpServletRequest request)
	{
		String baseHref = Tools.getBaseHref(request)+"/";
		return (send(fromName, fromEmail, toEmail, null, null, null, subject, text, baseHref, null));
	}

	public static boolean send(String fromName, String fromEmail, String toEmail, String subject, String text, String attachments)
	{
		return (send(fromName, fromEmail, toEmail, null, null, subject, text, attachments));
	}

	public static boolean send(String fromName, String fromEmail, String toEmail, String ccEmail, String bccEmail, String subject, String text, String attachments)
	{
		return(send(fromName, fromEmail, toEmail, null, ccEmail, bccEmail, subject, text, null, attachments));
	}

	public static boolean send(String senderName, String senderEmail, String recipientEmail, String replyTo, String ccEmail, String bccEmail, String subject, String message, String baseHref, String attachmentsList)
	{
		return sendCapturingException(senderName, senderEmail, recipientEmail, replyTo, ccEmail, bccEmail, subject, message, baseHref, attachmentsList).first;
	}

	 public static Pair<Boolean, Exception> sendCapturingException(String senderName, String senderEmail, String recipientEmail, String replyTo, String ccEmail, String bccEmail, String subject, String message, String baseHref, String attachmentsList)
	 {
		  return sendCapturingException(senderName, senderEmail, recipientEmail, replyTo, ccEmail, bccEmail, subject, message, baseHref, attachmentsList, true);
	 }

	 public static Pair<Boolean, Exception> sendCapturingException(String senderName, String senderEmail, String recipientEmail, String replyTo, String ccEmail, String bccEmail, String subject, String message, String baseHref, String attachmentsList, boolean sendLaterWhenException)
	 {
		  return sendCapturingException(senderName, senderEmail, recipientEmail, replyTo, ccEmail, bccEmail, subject, message, baseHref, attachmentsList, sendLaterWhenException, true);
	 }

	/**
	 * Odoslanie emailu
	 * @param senderName - meno odosielatela emailu
	 * @param senderEmail - email odosielatela emailu
	 * @param recipientEmail - email prijemcu
	 * @param replyTo - email pre pole replyTo, alebo null
	 * @param subject - predmet
	 * @param message - body emailu, moze byt v HTML formate, vratane odkazov na obrazky a linky
	 * @param baseHref - base HTTP adresa (aby bolo mozne nalinkovat obrazky a spravit relativne linky)
	 * @param attachmentsList - zoznam priloh vo formate url_adresa1;nazov_suboru_do_emailu1;url_adresa2;nazov_suboru_do_emailu2;
	 * @param sendLaterWhenException - ak je true, tak pri exception pri odosielani, ulozi e-mail pre neskorsie odoslanie
	 * @param writeToAuditLog - ak je false, tak sa posle len e-mail, ale nezapise sa nic do auditlogu
	 * @return true, ak sa email podarilo odoslat, inak false
	 * @throws Exception
	 */
	public static Pair<Boolean, Exception> sendCapturingException(String senderName, String senderEmail, String recipientEmail, String replyTo, String ccEmail, String bccEmail, String subject, String message, String baseHref, String attachmentsList, boolean sendLaterWhenException, boolean writeToAuditLog) {
		MailHelper mailHelper = new MailHelper()
				.setFromName(senderName)
				.setFromEmail(senderEmail)
				.setToEmail(recipientEmail)
				.setReplyTo(replyTo)
				.setCcEmail(ccEmail)
				.setBccEmail(bccEmail)
				.setSubject(subject)
				.setMessage(message)
				.setBaseHref(baseHref)
				.setAttachments(attachmentsList)
				.setSendLaterWhenException(sendLaterWhenException)
				.setWriteToAuditLog(writeToAuditLog);

		return sendCapturingException(mailHelper);
	}

	public static Pair<Boolean, Exception> sendCapturingException(MailHelper mailHelper)
	{
		String senderName = mailHelper.getFromName();
		String senderEmail = mailHelper.getFromEmail();
		String recipientEmail = mailHelper.getToEmail();
		String replyTo = mailHelper.getReplyTo();
		String ccEmail = mailHelper.getCcEmail();
		String bccEmail = mailHelper.getBccEmail();
		String subject = mailHelper.getSubject();
		String message = mailHelper.getMessage();
		String baseHref = mailHelper.getBaseHref();
		String attachmentsList = mailHelper.getAttachments();
		boolean sendLaterWhenException = mailHelper.isSendLaterWhenException();
		boolean writeToAuditLog = mailHelper.isWriteToAuditLog();
		List< Pair<String, String> > headers = mailHelper.getHeaders();

		if ("false".equals(Constants.getString("useSMTPServer")) && writeToAuditLog) // mail neodosleme ale ulozime do db pre \odoslanie na inom node, okrem pripadu, ze potrebujeme odoslat chybu sposobenu pri dosiahnuti maximalenho poctu DB spojeni
		{
			Logger.debug(SendMail.class, "useSMTPServer=false -> sending later. " );
			boolean result = sendLater(senderName, senderEmail, recipientEmail, replyTo, ccEmail, bccEmail, subject, message, baseHref, null, null, attachmentsList);
			return Pair.of(result, null);
		}

		//debugMail
		if (Tools.isNotEmpty(Constants.getString("debugEmail")))
		{
			Logger.debug(SendMail.class, "debugEmail="+Constants.getString("debugEmail"));
			recipientEmail = Constants.getString("debugEmail");
		}
		//pridanie ContextPath pre admin cast (ak je nastavene)
		if (Tools.isNotEmpty(Constants.getString("contextPathAdmin")) && message.indexOf("://cms")!=-1)
		{
			message = ContextFilter.addContextPath(Constants.getString("contextPathAdmin"), message);
			subject = ContextFilter.addContextPath(Constants.getString("contextPathAdmin"), subject);
		}

		Prop prop = Prop.getInstance(Constants.getString("defaultLanguage"));

		//vynimka helpdesk@websupport.sk je kvoli Cloudu a notifikaciam na WebSupport kedy nam odmietaju emaili z noreply@webjet.eu spracovat
		if (Tools.isEmail(Constants.getString(EMAIL_PROTECTION_SENDER_KEY)) && "helpdesk@websupport.sk".equals(recipientEmail)==false)
		{
			String from = Constants.getString(EMAIL_PROTECTION_SENDER_KEY);
			String oldSender = senderEmail;
			senderEmail = from;

			String defaultName = Constants.getString(EMAIL_PROTECTION_SENDER_NAME_KEY);
			if (Tools.isNotEmpty(defaultName)) {
				if (defaultName.equalsIgnoreCase("same-as-email")) {
					senderName = from;
				}
				else {
					senderName = defaultName;
				}
			}

			if (!from.equals(oldSender))
			{
				if (Tools.isEmpty(replyTo))
					replyTo = oldSender;
				//else - replyTo nemoze obsahovat viacero email adries!
				//	replyTo += ","+oldSender;
			}
		}

		RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
		if (requestBean != null)
		{
			if (Tools.isEmpty(baseHref)) baseHref = requestBean.getBaseHref();
		}
		if (Tools.isEmpty(baseHref)) try { baseHref = Tools.getBaseHref(null); } catch (Exception ex) {}

		//ak je <img src='xxxx' tak sa to nezobrazi v mozille
		try
		{
			int start = message.indexOf("src='");
			int end;
			while (start != -1)
			{
				end = message.indexOf("'", start+6);
				if (end > start)
				{
					message = message.substring(0, start) + "src=\"" + message.substring(start+5, end) + "\"" + message.substring(end+1);
					//Logger.println(this,message);
				}
				else
				{
					//ukonci cyklus
					break;
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if (baseHref!=null && baseHref.length()>8)
		{
			//uprav to z adresy http://www.interway.sk/nieco/stranka.html na http://www.interway.sk
			int i = baseHref.indexOf('/', 9);
			if (i!=-1)
			{
				baseHref = baseHref.substring(0, i);
				Logger.println(SendMail.class,"baseHref: " + baseHref);
			}
		}

		//oprav linky
		message = SendMail.createAbsolutePath(message, baseHref);

		Properties props = System.getProperties();
		props.put("mail.smtp.host", Constants.getString("smtpServer"));
        props.put("mail.mime.charset", SetCharacterEncodingFilter.getEncoding());

		Session ms = getSession(props);


		try
		{
			//ak nie je zadany odosielatel, domyslime si ho
			if (Tools.isEmpty(senderEmail))
			{
				senderEmail = recipientEmail;
			}

			//uprav email formatu lubos@balat.sk,pokus@interway.sk pretoze
			//niekedy je sender rovnaky ako prijemca
			senderEmail = getFirstEmailAddress(senderEmail);
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}

		if (subject.indexOf("WebJET restart")==-1)
		{

			//Logger.println(this,"SendMailMultipart from:"+senderName+" <"+senderEmail+"> to:"+recipientEmail);
			Logger.debug(SendMail.class,"SendMail["+Constants.getInstallName()+"]: sending email from: " + senderEmail + " to: " + recipientEmail+" subject: "+subject+"|");

			if ("cloud".equals(Constants.getInstallName()))
			{
				Logger.debug(SendMail.class, "email from:" + senderEmail + " to:" + recipientEmail+" subject:"+subject + " body:"+message);
			}

			if(writeToAuditLog)
			{
				 if ("cloud".equals(Constants.getInstallName()) && message.indexOf("Heslo:")==-1 && message.indexOf("Password:")==-1)
				 {
					  //pre cloud (mimo dmailu) logujeme aj telo emailu
					  Adminlog.add(Adminlog.TYPE_SENDMAIL, "email from:" + senderEmail + " to:" + recipientEmail+" subject:"+subject + " body:"+message, -1, -1);
				 }
				 else
				 {
					  Adminlog.add(Adminlog.TYPE_SENDMAIL, "email from:" + senderEmail + " to:" + recipientEmail+" subject:"+subject, -1, -1);
				 }
			}
		}

		try
		{
			Message mes = new MimeMessage(ms);


			if (subject.startsWith(AUTOREPLY_SUBJECT))
			{
			    mes.setHeader("X-Autoreply",AUTOREPLY_SUBJECT);
			}

			if (headers != null && headers.size() > 0)
			{
				for (Pair<String, String> header : headers)
				{
					mes.setHeader(header.getFirst(), header.getSecond());
				}
			}

			if (senderEmail.length() > 0)
			{
				mes.setFrom(new InternetAddress(senderEmail, senderName));
			}
			else
			{
				mes.setFrom(new InternetAddress("bez.emailu@interway.sk"));
			}

			//mes.setFrom(new InternetAddress(senderEmail, DB.internationalToEnglish(senderName)));
			mes.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));
			try
			{
				if (replyTo != null && replyTo.indexOf('@') != -1)
				{
					InternetAddress[] rt = new InternetAddress[1];
					replyTo = getFirstEmailAddress(replyTo);
					rt[0] = new InternetAddress(DB.internationalToEnglish(replyTo));
					mes.setReplyTo(rt);
				}
				if (ccEmail != null && ccEmail.indexOf('@')!=-1)
				{
					InternetAddress[] ccAddrs = InternetAddress.parse(ccEmail, false);
					mes.setRecipients(Message.RecipientType.CC, ccAddrs);
				}
				if (bccEmail != null && bccEmail.indexOf('@')!=-1)
				{
					InternetAddress[] bccAddrs = InternetAddress.parse(bccEmail, false);
					mes.setRecipients(Message.RecipientType.BCC, bccAddrs);
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}

			String emailEncoding = Constants.getString("emailEncoding");
			if (Tools.isEmpty(emailEncoding))
			{
				emailEncoding = SetCharacterEncodingFilter.getEncoding();
			}

			mes.setSubject(MimeUtility.encodeText(subject, emailEncoding, null));
			mes.setSentDate(new java.util.Date());


			if (isHtmlContent(message) || Tools.isNotEmpty(attachmentsList))
			{
				Multipart mpart = new MimeMultipart("mixed");
				Multipart attachments = new MimeMultipart();
				Multipart related = new MimeMultipart();
				message = putInlineAttachments(attachments, related, message, baseHref);

				boolean attachFiles = true;
				String serverPath;
				String fileName;
				StringTokenizer st = null;
				long size = 0;
				if (Tools.isNotEmpty(attachmentsList))   //skontroluj normalne attachmenty
				{
					st = new StringTokenizer(attachmentsList, ";");

					IwcmFile file = null;
					while (st.hasMoreTokens() && attachFiles)   //zisti velkosti priloh
					{
						serverPath = st.nextToken();
						if (st.hasMoreTokens())
						{
							file = IwcmFile.fromVirtualPath(serverPath);
							//try absolute path otherwise
							if (!file.exists()) file = new IwcmFile(serverPath);

							size += file.length();
							if (size > Constants.getLong("maxSizeOfAttachments"))
							{
								attachFiles = false;   //ak je velkost prilohy vacsia ako stanovena hranica, prilohy k mailu nepripojim
							}
						}

					}
				}
				if (!attachFiles)
				{
					message += prop.getText("email.too_large_attachments");
				}

				// create wrap if we have related attachments - images in html
				if (related.getCount() > 0)
				{
					Logger.println(SendMail.class, "mam nejake related, posielam");
					putMessagePart(mpart, message, related);
				} else
				{
					putMessagePart(mpart, message, null);
				}
				for (int i = 0; i < attachments.getCount(); i++)
				{
					mpart.addBodyPart(attachments.getBodyPart(i));
				}
				//nahod normalne attachmenty
				if (Tools.isNotEmpty(attachmentsList))
				{
					if (attachFiles)
					{
						st = new StringTokenizer(attachmentsList, ";");
						while (st.hasMoreTokens())
						{
							serverPath = st.nextToken();
							if (st.hasMoreTokens())
							{
								fileName = st.nextToken();
								SendMail.attFile(serverPath, fileName, mpart);
							}
						}
					}
				}

				mes.setContent(mpart);
			}
			else
			{
				//je to plain text email
				mes.setContent(message, "text/plain; charset="+emailEncoding);
			}

			try
			{
				if (Constants.getBoolean("sendMailSaveEmail")) {
					saveEmailToFile(mes);
				}
				else
				{
					Transport.send(mes);
				}
				Logger.debug(SendMail.class,"email odoslany");
				return Pair.of(true, null);
			}
			catch (Exception ex)
			{
				if (subject.indexOf("WebJET restart")==-1)
				{
					sk.iway.iwcm.Logger.error(ex);
					Adminlog.add(Adminlog.TYPE_SENDMAIL, "ERROR sending email (from: "+senderEmail+", to: "+recipientEmail+", cc: "+ccEmail+", bcc: "+bccEmail+", subject: "+subject+") ex:"+ex.getMessage()+"\n\n"+Logger.getStackTrace(ex), -1, -1);
					if (sendLaterWhenException) //mail ulozim pre neskorsie poslanie
					{
						Logger.debug(SendMail.class, "sendLaterWhenException=true -> sending later." );
						sendLater(senderName, senderEmail, recipientEmail, replyTo, ccEmail, bccEmail, subject, message, baseHref, null, null, attachmentsList);
					}
				}
				return Pair.of(false, ex);
			}
		}
		catch (Exception e)
		{
			if (subject.indexOf("WebJET restart")==-1)
			{
				sk.iway.iwcm.Logger.error(e);
                Adminlog.add(Adminlog.TYPE_SENDMAIL, "ERROR sending email (from: "+senderEmail+", to: "+recipientEmail+", cc: "+ccEmail+", bcc: "+bccEmail+", subject: "+subject+") ex:"+e.getMessage()+"\n\n"+Logger.getStackTrace(e), -1, -1);
			}
			return Pair.of(false, e);
		}
	}

	private static void saveEmailToFile(Message mes) {
		String sendMailSaveEmailPath = Constants.getString("sendMailSaveEmailPath", "");
		if (Tools.isEmpty(sendMailSaveEmailPath)) {
			Logger.debug(SendMail.class, "sendMailSaveEmailPath is not configured");
			return;
		}

		Logger.debug(SendMail.class, String.format("Email neodosielam, zapisujem do: %s", sendMailSaveEmailPath));
		String realPath = Tools.getRealPath(sendMailSaveEmailPath);
		if (!realPath.endsWith("/")) {
			realPath += "/";
		}

		File file = new File(realPath + new Date().getTime() + ".eml");
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			mes.writeTo(fileOutputStream);
			fileOutputStream.close();

			Logger.debug(SendMail.class, "Email zapisany");
		} catch (IOException | MessagingException e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	public static boolean sendLater(String senderName, String senderEmail, String recipientEmail, String replyTo, String ccEmail, String bccEmail, String subject, String message, String baseHref, String date, String time)
	{
		return sendLater(senderName, senderEmail, recipientEmail, replyTo, ccEmail, bccEmail, subject, message, baseHref, date, time,null);
	}

	/**
	 * Oneskorene odoslanie emailu
	 * @param senderName
	 * @param senderEmail
	 * @param recipientEmail
	 * @param replyTo
	 * @param subject
	 * @param message
	 * @param baseHref
	 * @param date
	 * @param time
	 * @return
	 */
	public static boolean sendLater(String senderName, String senderEmail, String recipientEmail, String replyTo, String ccEmail, String bccEmail, String subject, String message, String baseHref, String date, String time,String attachments)
	{
		//pridanie ContextPath pre admin cast (ak je nastavene)
		if (Tools.isNotEmpty(Constants.getString("contextPathAdmin")) && message.indexOf("://cms")!=-1)
		{
			message = ContextFilter.addContextPath(Constants.getString("contextPathAdmin"), message);
			subject = ContextFilter.addContextPath(Constants.getString("contextPathAdmin"), subject);
		}

		//uloz si parametre do DB a posli to v stanovenom case
		try
		{
			Connection db_conn = DBPool.getConnection();
			try
			{
				PreparedStatement ps = db_conn.prepareStatement("INSERT INTO emails (recipient_email, recipient_name, sender_name, sender_email, subject, url, attachments, retry, sent_date, created_by_user_id, create_date, send_at, message, reply_to, cc_email, bcc_email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				try
				{
					int counter = 1;
					ps.setString(counter++, recipientEmail);
					ps.setString(counter++, recipientEmail);
					ps.setString(counter++, senderName);
					ps.setString(counter++, senderEmail);
					ps.setString(counter++, subject);
					ps.setString(counter++, (baseHref == null) ? "" : baseHref);
					if (Tools.isEmpty(attachments))
					{
						ps.setString(counter++, null);
					}
					else
					{
						ps.setString(counter++, attachments);
					}
					ps.setInt(counter++, 0);
					ps.setNull(counter++, Types.TIMESTAMP);
					ps.setInt(counter++, -1);
					ps.setTimestamp(counter++, new Timestamp(Tools.getNow()));
					if (Tools.isNotEmpty(date) && Tools.isNotEmpty(time))
					{
						ps.setTimestamp(counter++, new Timestamp(DB.getTimestamp(date, time)));
					}
					else
					{
						ps.setNull(counter++, Types.TIMESTAMP);
					}
					DB.setClob(ps, counter++, message);
					ps.setString(counter++, replyTo);
					ps.setString(counter++, ccEmail);
					ps.setString(counter++, bccEmail);
					ps.execute();
				}
				finally { ps.close(); }
			}
			finally { db_conn.close(); }
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
			Adminlog.add(Adminlog.TYPE_SENDMAIL, "ERROR sending email later ex:"+ex.getMessage(), -1, -1);
		}

		return(true);
	}

	/**
	 * Ziska zoznam inline priloh (obrazkov)
	 * @param htmlCode
	 * @return
	 */
	public static List<String> getInlineAttachments(String htmlCode)
	{
		//Logger.println(this,"Getting INLINE attachments from mail");
		EditorKit kit = new HTMLEditorKit();
		Document doc = kit.createDefaultDocument();
		// The Document class does not yet
		// handle charset's properly.
		doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);

		List<String> att = new ArrayList<>();
		try
		{
			// Create a reader on the HTML content.
			Reader rd = new StringReader(htmlCode);
			// Parse the HTML.
			kit.read(rd, doc, 0);
			// Iterate through the elements
			// of the HTML document.
			ElementIterator it = new ElementIterator(doc);
			javax.swing.text.Element elem;
			while ((elem = it.next()) != null)
			{
				if (elem.getName().equals("img"))
				{
					String src = (String) elem.getAttributes().getAttribute(HTML.Attribute.SRC);
					if (Tools.isEmpty(src))
					{
						continue;
					}
					//Logger.println(this,src);
					att.add(src);
				}
				//jeeff: test na atribut background
				if (elem.getAttributes().getAttribute(HTML.Attribute.BACKGROUND) != null)
				{
					String src = (String) elem.getAttributes().getAttribute(HTML.Attribute.BACKGROUND);
					if (Tools.isEmpty(src))
					{
						continue;
					}
					//Logger.println(this,src);
					att.add(src);
				}
			}
		}
		catch (Exception e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
		return(att);
	}

	/**
	 * Prida do emailu Inline obrazky
	 * @param atts
	 * @param related
	 * @param message
	 * @param baseHref
	 * @return
	 * @throws Exception
	 */
	private static String putInlineAttachments(Multipart atts, Multipart related, String message, String baseHref) throws Exception
	{
		String rt = message;
		try
		{
			List<String> att = getInlineAttachments(message);

			Iterator<String> iter = att.iterator();
			String url;
			IwcmFile f;
			FileDataSource fds;
			URL u;
			URLDataSource uds;
			MimeBodyPart at;
			while (iter.hasNext())
			{
				url = iter.next();
				if (url.startsWith("http"))
				{
					//url = sk.iway.URLPathEncoder.encode(url);
					url = Tools.replace(url, " ", "%20");

					//inline attachment
					String downloadUrl = url;
					if (url.indexOf("http")==-1)
					{
						downloadUrl = baseHref + url;
					}
					downloadUrl = Tools.natUrl(downloadUrl);
					u = new URL(downloadUrl);

					Logger.println(SendMail.class,"ATT url: " + downloadUrl);
					uds = new URLDataSource(u);
					at = new MimeBodyPart();
					at.setDataHandler(new DataHandler(uds));
				}
				else
				{
					if (url.charAt(0)!='/')
					{
						url = "/" + url; //NOSONAR
					}

					//ak mame multidomain fixni cesty
					String localUrl = url;
					//tento try neodstranujte, moze nastat indexOufOfBounds v casti baseHref.indexOf
					try
					{
						if (Tools.isNotEmpty(baseHref))
						{
							String domain = baseHref.substring(baseHref.indexOf("://"));
							if (domain.indexOf(':')!=-1) domain = domain.substring(0, domain.indexOf(':'));
							localUrl = MultiDomainFilter.rewriteUrlToLocal(url, MultiDomainFilter.getDomainAlias(domain));
							Logger.debug(SendMail.class, "Trying to fix multidomain: url="+url+" local="+localUrl+" domain="+domain+" basehref="+baseHref);
						}
					}
					catch (Exception ex)
					{
						sk.iway.iwcm.Logger.error(ex);
					}

					f = new IwcmFile((Tools.getRealPath(localUrl)));

					if (IwcmFsDB.useDBStorage(localUrl) && f.exists())
					{
						File tempFile = new File(IwcmFsDB.getTempFilePath(f.getPath()));
						if (tempFile.exists()==false || tempFile.lastModified() > f.lastModified())
						{
							IwcmFsDB.writeFileToDisk(new File(f.getPath()), tempFile);
						}
					}

					if (f.exists() && f.canRead() && f.isFile())
					{
						Logger.println(SendMail.class,"ATT file: " + url + " path="+f.getAbsolutePath());
						if (IwcmFsDB.useDBStorage(url))
						{
						fds = new FileDataSource(new File(IwcmFsDB.getTempFilePath(f.getPath())));
						}
						else
						{
							fds = new FileDataSource(new File(f.getAbsolutePath()));
						}

						at = new MimeBodyPart();
						at.setDataHandler(new DataHandler(fds));
					}
					else
					{
						//je to nejaky virtualny obrazok, je mozne ho asi len stiahnut
						url = Tools.replace(url, " ", "%20");

						//inline attachment
						String downloadUrl = url;
						if (url.indexOf("http")==-1)
						{
							downloadUrl = baseHref + url;
						}

						Logger.debug(SendMail.class,"ATT url: " + downloadUrl);

						downloadUrl = Tools.natUrl(downloadUrl);
						u = new URL(downloadUrl);

						uds = new URLDataSource(u);
						at = new MimeBodyPart();
						at.setDataHandler(new DataHandler(uds));
					}
				}

				//ponechame povodne URL obrazku
				boolean dmailDisableInlineImages = Constants.getBoolean("dmailDisableInlineImages");
				String trackGif = Constants.getString("dmailTrackopenGif");

				boolean whitelisted = false;
				String dmailWhitelistImageDomains = Constants.getString("dmailWhitelistImageDomains");
				if (Tools.isNotEmpty(dmailWhitelistImageDomains) && url.startsWith("http"))
				{
					try
					{
						String domain = url.substring(url.indexOf(":")+3, url.indexOf("/", 8));
						Logger.debug(SendMail.class, "Testing whitelist domain:"+domain);
						if (dmailWhitelistImageDomains.indexOf(domain)!=-1) whitelisted = true;
					}
					catch (Exception e)
					{
						sk.iway.iwcm.Logger.error(e);
					}
				}

				if(dmailDisableInlineImages || (Tools.isNotEmpty(trackGif) && url.indexOf(trackGif)!=-1) || whitelisted)
				{
					if (url.indexOf("http")==-1)
					{
						url = Tools.replace(url, "%20", " ");
						String newUrl = baseHref + url;
						rt = Tools.replace(rt, url, newUrl);
					}
				}
				else
				{
					String hash = putAsInline(rt, url);
					Logger.println(SendMail.class,"hash: " + hash);
					if (hash != null && at != null)
					{
						rt = Tools.replace(rt, url, "cid:WebJET." + hash);

						String onlyFile = onlyFile(url);
						if (onlyFile!=null && onlyFile.toLowerCase().endsWith(".png"))
						{
							//toto linux server niekedy nepozna
							at.setHeader("Content-Type", "image/png; name="+onlyFile);
						}

						at.setHeader("Content-ID", "<WebJET." + hash + ">");
						at.setHeader("Content-Disposition", "inline;\n filename=\""+onlyFile+"\"");
						related.addBodyPart(at);
						Logger.println(SendMail.class,"pridane");
					}
				}
			}
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		finally
		{

		}
		return rt;
	}

	/**
	 * Prida do emailu message part
	 * @param mp
	 * @param message
	 * @param related
	 * @throws MessagingException
	 */
	private static void putMessagePart(Multipart mp, String message, Multipart related) throws MessagingException
	{
		MimeMultipart content = new MimeMultipart("alternative");
		String txtVersion = isHtmlContent(message) ? sk.iway.Html2Text.html2text(message) : message;

		String emailEncoding = Constants.getString("emailEncoding");
		if (Tools.isEmpty(emailEncoding))
		{
			emailEncoding = SetCharacterEncodingFilter.getEncoding();
		}

		if (txtVersion != null && txtVersion.length() > 3)
		{
			MimeBodyPart text = new MimeBodyPart();

			txtVersion = Tools.replace(txtVersion, "&lt;", "<");
			txtVersion = Tools.replace(txtVersion, "&gt;", ">");

			//outlook maze nove riadky, fixuje sa to pridanim 2 medzier na kazdy novy riadok
			//http://www.masternewmedia.org/newsletter_publishing/newsletter_formatting/remove_line_breaks_issue_Microsoft_Outlook_2003_when_publishing_text_newsletters_20051217.htm
			txtVersion = Tools.replace(txtVersion, "\r", "");
			txtVersion = Tools.replace(txtVersion, "\n", "\n  ");
			if (txtVersion.startsWith("  ")==false) txtVersion = "  "+txtVersion;

			text.setText(txtVersion, emailEncoding);
			text.setHeader("MIME-Version", "1.0");
			text.setHeader("Content-Type", "text/plain; charset="+emailEncoding);
			//text.setContent(message, "text/plain; charset=windows-1250");
			//mp.addBodyPart(text);
			content.addBodyPart(text);
		}
		if (message != null) {
			String det = message.toLowerCase();
			if (isHtmlContent(det))
			{
				if (det.indexOf("<html")==-1)
				{
					message = "<html><body>" + message + "</body></html>";
				}

				if (related != null && related.getCount() > 0)
				{
					Multipart rela = new MimeMultipart("related");
					MimeBodyPart html = new MimeBodyPart();
					html.setContent(message, "text/html; charset="+emailEncoding);
					html.setHeader("MIME-Version", "1.0");
					html.setHeader("Content-Type", "text/html; charset="+emailEncoding);
					rela.addBodyPart(html);
					for (int i = 0; i < related.getCount(); i++)
					{
						rela.addBodyPart(related.getBodyPart(i));
					}
					MimeBodyPart wrap = new MimeBodyPart();
					wrap.setContent(rela);
					content.addBodyPart(wrap);
				}
				else
				{
					MimeBodyPart html = new MimeBodyPart();
					html.setContent(message, "text/html; charset="+emailEncoding);
					html.setHeader("MIME-Version", "1.0");
					html.setHeader("Content-Type", "text/html; charset="+emailEncoding);
					//mp.addBodyPart(html);
					content.addBodyPart(html);
				}
			}
		}
		//mp.setContent(content);
		//mp.addBodyPart(content);
		MimeBodyPart wrap = new MimeBodyPart();
		wrap.setContent(content);
		// HERE'S THE KEY
		mp.addBodyPart(wrap);
	}

	/**
	 * Otestuje, ci zadany test je HTML kod
	 * @param html
	 * @return
	 */
	public static boolean isHtmlContent(String html)
	{
		if (Tools.isNotEmpty(html)) {
			String htmlLC = html.toLowerCase();
			if (htmlLC.indexOf("<br")!=-1) return true;
			if (htmlLC.indexOf("<div>")!=-1) return true;
			if (htmlLC.indexOf("</p>")!=-1) return true;
			if (htmlLC.indexOf("</h")!=-1) return true;
			if (htmlLC.indexOf("</a>")!=-1) return true;
			if (htmlLC.indexOf("<img ")!=-1) return true;
			if (htmlLC.indexOf("</b>")!=-1) return true;
			if (htmlLC.indexOf("</strong>")!=-1) return true;
			if (htmlLC.indexOf("</ul>")!=-1) return true;
			if (htmlLC.indexOf("</ol>")!=-1) return true;
			if (htmlLC.indexOf("</td>")!=-1) return true;
		}

		return false;
	}

	/**
	 * Vypocita HASH hodnotu pre inline prilohu
	 * @param message
	 * @param iurl
	 * @return
	 */
	private static String putAsInline(String message, String iurl)
	{
		String rt = null;
		if (message.indexOf(iurl) != -1)
		{
			rt = String.valueOf(iurl.hashCode());
			// nemozeme pouzit replace all pretoze "regular expression" je aktivne
			// aj
			// na X? a to moze byt sucast URL
			// message = replaceAll(message,iurl,"cid:"+rt);
		}
		return rt;
	}


	/**
	 * Vrati nazov suboru z cesty
	 * @param f
	 * @return
	 */
	private static String onlyFile(String f)
	{
		try
		{
			//odstran image?v=aaaa z URL
			int questionMark = f.indexOf("?");
			if (questionMark > 0)
			{
				f = f.substring(0, questionMark);
			}

			f = java.net.URLDecoder.decode(f, SetCharacterEncodingFilter.getEncoding());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}
		f = f.replace('\\', '/');
		int pos = f.lastIndexOf('/');
		if (pos == -1)
		{
			return(DocTools.removeChars(f));
		}
		return(DocTools.removeChars(f.substring(pos + 1)));
	}


	/**
	 *  Description of the Method
	 *
	 *@param  serverPath            Description of the Parameter
	 *@param  fileName              Description of the Parameter
	 *@param  mp  Description of the Parameter
	 */
	public static void attFile(String serverPath, String fileName, Multipart mp)
	{
		if (serverPath.contains("dms")){
            //toto nemoze byt povolene, pretoze to nepojde na WJ kde nie su DMS triedy, treba prerobit na reflectin DmsNotify.attDmsFile(fileName,mp);
		}
		else
		{
			Logger.println(SendMail.class,"attaching: "+serverPath+" fileName="+fileName);
			try
			{
				IwcmFile file = IwcmFile.fromVirtualPath(serverPath);
				//try absolute path otherwise
				if (!file.exists()) file = new IwcmFile(serverPath);

				if (file.exists())
				{
					MimeBodyPart mbp2 = new MimeBodyPart();

					FileDataSource fds = null;

					if (IwcmFsDB.useDBStorage(IwcmFsDB.getVirtualPath(file.getAbsolutePath())))
					{
						IwcmFsDB.writeFileToDisk(new File(file.getAbsolutePath()),new File(IwcmFsDB.getTempFilePath(file.getAbsolutePath())));
						fds=new FileDataSource(IwcmFsDB.getTempFilePath(file.getAbsolutePath()));
					}
					else
					{
						fds=new FileDataSource(file.getAbsolutePath());
					}

					String mimeType = Constants.getServletContext().getMimeType(file.getName().toLowerCase());
					if (Tools.isEmpty(mimeType)) mimeType = "application/octet-stream";

					mbp2.setDataHandler(new DataHandler(fds));
					mbp2.setHeader("Content-Type", mimeType);
					String emailEncoding = Constants.getString("emailEncoding");
					if (Tools.isEmpty(emailEncoding))
						emailEncoding = SetCharacterEncodingFilter.getEncoding();
					mbp2.setFileName(MimeUtility.encodeText(Tools.isNotEmpty(fileName) ? fileName : file.getName(), emailEncoding, null));

					mp.addBodyPart(mbp2);

					Logger.println(SendMail.class,"done");
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
		}

	}

	/**
	 * Vrati kodovanie pre email podla nastavenia servera, alebo ak nie je prazdne podla konfiguracnej premennej emailEncoding
	 * @return
	 */
	public String getEncoding()
	{
		String emailEncoding = Constants.getString("emailEncoding");
		if (Tools.isEmpty(emailEncoding))
		{
			emailEncoding = SetCharacterEncodingFilter.getEncoding();
		}
		return emailEncoding;
	}

	/**
	 * Vytvori absolutne cesty v zadanom HTML kode
	 * @param htmlCode - HTML kod
	 * @param basePath - absolutna adresa, bez koncoveho /, napr. http://www.iway.sk
	 * @return
	 */
	public static String createAbsolutePath(String htmlCode, String basePath)
	{
		if (basePath == null)
		{
			return(htmlCode);
		}

		//Logger.println(this,"replace: " + htmlCode);

		//ak je tam uz base path, tak ju zrus, inak tam bude 2x
		//htmlCode = Tools.replace(htmlCode, basePath, "");
		//htmlCode = Tools.replace(htmlCode, "url(images/", "url("+basePath+"/images/");
		//htmlCode = Tools.replace(htmlCode, "url(/images/", "url("+basePath+"/images/");
		//htmlCode = Tools.replace(htmlCode, "\"/images/", "\""+basePath+"/images/");
		//htmlCode = Tools.replace(htmlCode, "\"/files/", "\""+basePath+"/files/");
		//htmlCode = Tools.replace(htmlCode, "\"/css/", "\""+basePath+"/css/");
		//htmlCode = Tools.replace(htmlCode, "/showdoc.do", "\""+basePath+"/showdoc.do");
		htmlCode = Tools.replace(htmlCode, "href=\"/", "href=\"" + basePath + "/");
		htmlCode = Tools.replace(htmlCode, "href='/", "href='" + basePath + "/");
		htmlCode = Tools.replace(htmlCode, "action=\"/", "action=\"" + basePath + "/");
		htmlCode = Tools.replace(htmlCode, "action='/", "action='" + basePath + "/");

		//replace URL v texte
		htmlCode = Tools.replace(htmlCode, "target=\"_blank\">/", "target=\"_blank\">"+basePath+"/");

		//Logger.println(this,"-------replaced: " + htmlCode);

		return(htmlCode);
	}

	public static String createAbsolutePath(String htmlCode, HttpServletRequest request)
	{
		String basePath = Tools.getBaseHref(request);
		return(createAbsolutePath(htmlCode, basePath));
	}

	/**
	 * @param props
	 * @return
	 */
	public static Session getSession(Properties props)
	{
		if (Constants.getBoolean("useAmazonSES")==false) {
			String port = "25";
			if (Tools.isNotEmpty(Constants.getString("smtpPort")))
			{
				props.setProperty("mail.smtp.port", Constants.getString("smtpPort"));
				port=Constants.getString("smtpPort");
			}
			else
			{
				props.setProperty("mail.smtp.port", port);
			}

			int smtpConnectionTimeoutMillis = Constants.getInt("smtpConnectionTimeoutMillis");
			if(smtpConnectionTimeoutMillis > 0)
			{
				props.put("mail.smtp.connectiontimeout", smtpConnectionTimeoutMillis);
			}

			if (Constants.getBoolean("smtpUseSSL"))
			{
				props.put("mail.smtp.socketFactory.port", port);
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				props.put("mail.smtp.ssl.checkserveridentity", true);
				props.put("mail.smtp.socketFactory.fallback", "false");
			}
			// office365 (ak je port 587, smtpUseTLS musi byt true)
			else if(Constants.getBoolean("smtpUseTLS"))
			{
				props.put("mail.smtp.starttls.enable", "true");
				String smtpTLSVersion = Constants.getString("smtpTLSVersion");
				if (Tools.isNotEmpty(smtpTLSVersion)) props.put("mail.smtp.ssl.protocols", smtpTLSVersion);
			}
		}

		Session ms;
		if (Tools.isNotEmpty(Constants.getString("smtpUser")) &&  Tools.isNotEmpty(Constants.getString("smtpPassword")))
		{
			final class WJAuthenticator extends jakarta.mail.Authenticator
			{
				private final PasswordAuthentication authentication;

				public WJAuthenticator()
				{

					String username = Constants.getString("smtpUser");
					String password = Constants.getString("smtpPassword");
					authentication = new PasswordAuthentication(username, password);
				}

				@Override
				protected PasswordAuthentication getPasswordAuthentication()
				{
					return authentication;
				}
			}
			props.setProperty("mail.smtp.auth", "true");

			//ziskanie getDefaultInstance nam na Exchange vracalo chybu INFO: java.lang.SecurityException: Access to default session denied
			ms = Session.getInstance(props, new WJAuthenticator());
		}
		else if (Constants.getBoolean("useAmazonSES")
					&&  Tools.isNotEmpty(Constants.getString("amazonAccessKey"))
					&&  Tools.isNotEmpty(Constants.getString("amazonSecretKey"))) {
					props.put("mail.aws.user", Constants.getString("amazonAccessKey"));
					props.put("mail.aws.password", Constants.getString("amazonSecretKey"));

					if(Tools.isNotEmpty(Constants.getString("amazonEmailServiceHost"))) props.put("mail.aws.host", Constants.getString("amazonEmailServiceHost"));

					ms = Session.getDefaultInstance(props,null);
		} else {
			ms = Session.getDefaultInstance(props,null);
		}
		return ms;
	}

	/**
	 * Ziska prvu email adresu zo stringu typu meno@domena.sk,ine@domena.sk (pre polia, ktore mozu mat len jeden email ako from a replyTo)
	 * @param email
	 * @return
	 */
	public static String getFirstEmailAddress(String email) {
		int i = email.indexOf(',');
		if (i!=-1)
		{
			email = email.substring(0, i);
		}
		return email;
	}

	/**
	 * Return senderName of various email from config value moduleDefaultSenderName or defaultSenderName if module version is not defined.
	 * If config value is not defined, fallback is empty then domain name will be used.
	 * @param module
	 * @param fallbackName - if no defaultSenderName constants is defined this value will be used
	 * @return
	 */
	public static String getDefaultSenderName(String module, String fallbackName) {
		String senderName = Constants.getStringExecuteMacro(module + "DefaultSenderName");
		if (Tools.isEmpty(senderName)) senderName = Constants.getStringExecuteMacro("defaultSenderName");
		if (Tools.isEmpty(senderName)) {
			senderName = fallbackName;
		}
		if (Tools.isEmpty(senderName)) {
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (rb != null) {
				senderName = rb.getDomain().replace("https://", "").replace("http://", "").replace("www.", "");
			}
		}
		return senderName;
	}

	/**
	 * Return senderEmail of various email from config value moduleDefaultSenderEmail or defaultSenderEmail if module version is not defined.
	 * If config value is not defined, fallback is empty then domain name will be used.
	 * @param module
	 * @param fallbackEmail - if no defaultSenderEmail constants is defined this value will be used
	 * @return
	 */
	public static String getDefaultSenderEmail(String module, String fallbackEmail) {
		String senderEmail = Constants.getStringExecuteMacro(module + "DefaultSenderEmail");
		if (Tools.isEmpty(senderEmail)) senderEmail = Constants.getStringExecuteMacro("defaultSenderEmail");
		if (Tools.isEmpty(senderEmail)) {
			senderEmail = fallbackEmail;
		}
		if (Tools.isEmpty(senderEmail)) {
			RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
			if (rb != null) {
				senderEmail = "no-reply@"+rb.getDomain().replace("https://", "").replace("http://", "").replace("www.", "");
			}
		}
		return senderEmail;
	}
}