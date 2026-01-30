package sk.iway.iwcm.components.multistep_form.rest;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.DB;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.DocTools;
import sk.iway.iwcm.common.SearchTools;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsEntity;
import sk.iway.iwcm.components.form_settings.jpa.FormSettingsRepository;
import sk.iway.iwcm.components.forms.FormsEntity;
import sk.iway.iwcm.components.multistep_form.rest.SaveFormService.FormFiles;
import sk.iway.iwcm.form.FormDetails;
import sk.iway.iwcm.form.FormMailAction;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.users.UsersDB;

/**
 * Service responsible for preparing and sending emails for multi‑step forms.
 * <p>
 * Responsibilities:
 * - Extracts sender name and email from submitted form data based on configured field keys
 * - Composes HTML or plain‑text email body including optional CSS and attachments
 * - Applies repository settings (encoding, reply-to/cc/bcc, delayed send, double‑opt‑in)
 * - Sends email via configured SMTP or schedules deferred sending when SMTP is disabled
 */
@Service
public class FormMailService {

	public static final String NAME_FIELD_KEY = "multistepform_nameFields";
	public static final String EMAIL_FIELD_KEY = "multistepform_emailFields";

	private final FormSettingsRepository formSettingsRepository;

	@Autowired
	public FormMailService(FormSettingsRepository formSettingsRepository) {
		this.formSettingsRepository = formSettingsRepository;
	}

	/**
	 * Extracts values from the given form for fields whose names match the configured list
	 * defined by the provided {@code constant} key.
	 *
	 * @param form the submitted form entity containing serialized field data
	 * @param constant configuration key whose value is an array of field names
	 * @return ordered list of matching field values; empty list when none found or no data
	 */
    private List<String> getFieldsValues(FormsEntity form, String constant) {
        List<String> foundValues = new ArrayList<>();

        if(form.getData() == null) return foundValues;

        List<String> fieldsNames = Arrays.stream( Constants.getArray(constant) )
                                    .map(s -> s.toLowerCase())
                                    .toList();

        for(String combo : Tools.getTokens(form.getData(), "|")) {
            String comboArr[] = Tools.getTokens(combo, "~");
            if(comboArr.length != 2) continue;

			//
			comboArr[0] = comboArr[0].replaceFirst("-\\d+$", "");

            if(fieldsNames.contains(comboArr[0].toLowerCase()))
                foundValues.add(comboArr[1]);
        }

        return foundValues;
    }

	/**
	 * Sends a notification email for the given form submission.
	 * <p>
	 * Behavior overview:
	 * - Determines sender name/email from form data using configuration keys
	 * - Applies form settings (encoding, reply-to/cc/bcc, delayed sending, attachments handling)
	 * - Inlines CSS into HTML body when sending as HTML unless forced to plain text
	 * - Optionally attaches message HTML as a separate file when configured
	 * - Sends immediately via SMTP or schedules for later when SMTP is disabled
	 *
	 * @param form       form entity with metadata and serialized field data
	 * @param recipients comma‑separated list of recipient emails
	 * @param subject    email subject
	 * @param formFiles  uploaded files container to optionally attach
	 * @param attachFiles when true attaches uploaded files to the email
	 * @param cssData    inline <style> block or CSS links already made absolute
	 * @param htmlData   rendered form body HTML (will be transformed as needed)
	 * @param request    current HTTP request used for context and headers
	 */
    public void sendMail(FormsEntity form, String recipients, String subject, FormFiles formFiles, boolean attachFiles, String cssData, StringBuilder htmlData, HttpServletRequest request) {
		Prop prop = Prop.getInstance(request);
		FormSettingsEntity formSettings = formSettingsRepository.findByFormNameAndDomainId(form.getFormName(), CloudToolsForCore.getDomainId());

        String meno = null;
        List<String> namesList = getFieldsValues(form, NAME_FIELD_KEY);
        if(namesList.size() > 0) meno = namesList.stream().map(name -> DB.internationalToEnglish(name)).collect(Collectors.joining(" "));

        String email = null;
        List<String> emailsList = getFieldsValues(form, EMAIL_FIELD_KEY);
		//remove invalid emails
		emailsList = emailsList.stream().filter(e -> Tools.isEmail(e)).collect(Collectors.toList());
        if(emailsList.size() > 0) email = emailsList.get(0);

		String emailEncoding = SetCharacterEncodingFilter.getEncoding();
		String formMailEncoding = Constants.getString("formMailEncoding");
		if (Tools.isNotEmpty(formMailEncoding)) emailEncoding = formMailEncoding;
        if(Tools.isTrue(formSettings.getFormMailEncoding())) emailEncoding = "ASCII";

		String host = Constants.getString("smtpServer");
		boolean forceTextPlain = Tools.isTrue(formSettings.getForceTextPlain());

        int sendUserInfoDocId =  (formSettings.getFormMailSendUserInfoDocId() != null) ? formSettings.getFormMailSendUserInfoDocId() : -1;
		Logger.debug(FormMailService.class, "sendUserInfoDocId=" + sendUserInfoDocId + " email=" + email);

		// set as attribute so sendUserInfo would know
		request.setAttribute("doubleOptIn", formSettings.getDoubleOptIn());

		if (sendUserInfoDocId > 0)
			FormMailAction.sendUserInfo(sendUserInfoDocId, form.getId().intValue(), email, formFiles.getAttachs(), null, request);

		Logger.println(FormMailService.class,"FormMailService recipients=" + recipients);

		// Prepare Admninlog entry
		StringBuilder sb = new StringBuilder();
		sb.append("Form ").append(form.getFormName());
		String from = null;

        if ("nobody@nowhere.com".equals(recipients) == false && recipients != null && recipients.contains("@"))
		{
			if (Tools.isEmail(email) == false) {
				String emailProtectionSenderEmail = Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY);
				if (Tools.isEmail(emailProtectionSenderEmail)) {
					email = emailProtectionSenderEmail;
				} else {
					//skus nastavit odosielatela ako prijemcu
					String[] emails = recipients.split(",");
					if (emails != null && emails.length > 0) email = emails[0];
					else email = "webform@" + Tools.getServerName(request);
				}
			}

			if (meno == null || meno.trim().length() < 1) meno = email;

			if (emailEncoding.indexOf("ASCII") != -1) htmlData = new StringBuilder(DB.internationalToEnglish(htmlData.toString()));
			htmlData = new StringBuilder(FormMailAction.createAbsolutePath(htmlData.toString(), request));
			cssData = FormMailAction.createAbsolutePath(cssData, request);

			boolean sendMessageAsAttach = Tools.isTrue(formSettings.getMessageAsAttach());
			IwcmFile messageAsAttachFile = null;
			if(sendMessageAsAttach) {
				String messageAsAttachFileName = formSettings.getMessageAsAttachFileName();
				if(Tools.isEmpty(messageAsAttachFileName)) messageAsAttachFileName = Constants.getString("multistepform_attachmentDefaultName");
				else messageAsAttachFileName = DocTools.removeChars(messageAsAttachFileName);
				messageAsAttachFile = new IwcmFile(Tools.getRealPath(FormMailAction.FORM_FILE_DIR + File.separator + form.getId() + "_" + messageAsAttachFileName));
				FileTools.saveFileContent(messageAsAttachFile.getVirtualPath(), "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset="+emailEncoding+"\">"+(cssData == null ? "" : cssData)+"</head><body id=\"WebJETEditorBody\" class=\"WebJETMailBody\"><div class=\"WebJETMailWrapper\">\n\n"+htmlData+"\n\n</div></body></html>", emailEncoding);
			}

			if ("false".equals(Constants.getString("useSMTPServer"))) {
				if(sendMessageAsAttach && messageAsAttachFile!=null) {
					htmlData = new StringBuilder(prop.getText("form.formmailaction.pozrite_si_prilozeny_subor"));
					formFiles.getFileNamesSendLater().append(";").append(FormMailAction.FORM_FILE_DIR).append(messageAsAttachFile.getName()).append(";").append(messageAsAttachFile.getName());
				}

				if(formFiles.getAttachs() != null && !attachFiles) htmlData.append(prop.getText("email.too_large_attachments"));

				String messageBody = htmlData.toString();
				if (sendMessageAsAttach==false && forceTextPlain==false)
					messageBody = FormHtmlHandler.appendStyle(htmlData.toString(), cssData, emailEncoding, forceTextPlain);

				Adminlog.add(Adminlog.TYPE_FORMMAIL, "Formular " + form.getFormName() + " uspesne ulozeny do databazy, odoslany bude neskor", form.getDocId(), form.getId().intValue());

				//musime kvoli clustru a potencionalnemu zapisu suborov pozdrzat email
				long sendLaterTime = Tools.getNow();
				sendLaterTime += (5 * Constants.getInt("clusterRefreshTimeout"));

				SendMail.sendLater(meno, FormMailAction.getFirstEmail(email), recipients, formSettings.getReplyTo(), formSettings.getCcEmails(), formSettings.getBccEmails(), subject, messageBody, Tools.getBaseHref(request), Tools.formatDate(sendLaterTime), Tools.formatTime(sendLaterTime), formFiles.getFileNamesSendLater() != null ? "" : formFiles.getFileNamesSendLater().toString());
			} else {
				//vygeneruj mail a posli ho
				Properties props = System.getProperties();

				try {
					if (host != null && host.length() > 2)
						props.put("mail.smtp.host", host);
					else if (props.getProperty("mail.smtp.host") == null)
						props.put("mail.smtp.host", InetAddress.getLocalHost().getHostName());
				} catch (Exception ex) { Logger.error(FormMailService.class, ex); }

				Session session = SendMail.getSession(props);
				MimeMessage msg = new MimeMessage(session);
				InternetAddress[] toAddrs = null;

				try {
					//este potrebujeme updatnut linky na subory
					FormDetails formDetails = new FormDetails();
					formDetails.setFiles( String.join(",", formFiles.getFileNames().keySet()) );

					//updatni linky na attachementy
					String baseHref = Tools.getBaseHref(request);
					htmlData = Tools.replace(htmlData, "!ATTACHMENTS!", formDetails.getAttachements(baseHref));

					try {
						String proxyHost = request.getHeader("x-forwarded-for");
						if (proxyHost != null && proxyHost.length() > 4)
							msg.setHeader("X-sender-proxy", proxyHost);

						msg.setHeader("X-sender-ip", Tools.getRemoteIP(request));
						msg.setHeader("X-sender-host", Tools.getRemoteHost(request));
						msg.setHeader("X-server-name", Tools.getServerName(request));

						Identity user = UsersDB.getCurrentUser(request);
						if (user != null) {
							msg.setHeader("X-sender-userid", Integer.toString(user.getUserId()));
							msg.setHeader("X-sender-fullname", DB.internationalToEnglish(user.getFullName()));
						}
					} catch (Exception ex) { Logger.error(FormMailService.class, ex); }

					toAddrs = InternetAddress.parse(recipients, false);
					msg.setRecipients(Message.RecipientType.TO, toAddrs);

					String ccEmails = formSettings.getCcEmails();
					if (Tools.isEmail(ccEmails)) {
						InternetAddress[] ccAddrs = InternetAddress.parse(ccEmails, false);
						msg.setRecipients(Message.RecipientType.CC, ccAddrs);
					}

					String bccEmails = formSettings.getBccEmails();
					if (Tools.isEmail(bccEmails)) {
						InternetAddress[] bccAddrs = InternetAddress.parse(bccEmails, false);
						msg.setRecipients(Message.RecipientType.BCC, bccAddrs);
					}

					from = email;
					msg.setFrom(new InternetAddress(FormMailAction.getFirstEmail(email), meno));

					msg.setSubject(MimeUtility.encodeText(subject, emailEncoding, null));
					msg.setSentDate(new java.util.Date());


					MimeBodyPart html = new MimeBodyPart();

					if(sendMessageAsAttach) htmlData = new StringBuilder(prop.getText("form.formmailaction.pozrite_si_prilozeny_subor"));
					if(formFiles.getAttachs() != null && attachFiles == false) htmlData.append(prop.getText("email.too_large_attachments"));

					//odstran diakritiku
					if (emailEncoding.indexOf("ASCII")!=-1)
						htmlData = new StringBuilder(DB.internationalToEnglish(htmlData.toString()));

					//test ci je to HTML content, alebo nie. <br> za HTML content nepovazujem
					String htmlDataNoBR = Tools.replace(htmlData.toString(), "<br>", "\n");
					htmlDataNoBR = Tools.replace(htmlDataNoBR, "<br/>", "\n");
					if (htmlDataNoBR.indexOf('<') != -1 && htmlDataNoBR.indexOf('>') != -1)
						html.setContent(FormHtmlHandler.appendStyle(htmlData.toString(), cssData, emailEncoding, forceTextPlain), "text/html; charset="+emailEncoding);
					else
						html.setContent(htmlData.toString(), "text/plain; charset=" + emailEncoding);

					Multipart mp = new MimeMultipart("mixed");
					if ((forceTextPlain==false || (formFiles.getAttachs() != null && attachFiles)) && (sendMessageAsAttach || (htmlDataNoBR.indexOf('<') != -1 && htmlDataNoBR.indexOf('>') != -1))) {
						//#15245 - ak je nastaveny forceTextPlain a mal som aj prilohy, text formularu necham ako text/plain a prilohy pridam standartnym sposobom
						if(forceTextPlain)
							html.setContent(SearchTools.htmlToPlain(htmlDataNoBR), "text/plain; charset="+emailEncoding);

						mp.addBodyPart(html);

						if(formFiles.getAttachs() != null && attachFiles) {
							for(IwcmFile file : formFiles.getAttachs())
								FormMailAction.attFile(file, mp, emailEncoding);
						}

						if(sendMessageAsAttach) FormMailAction.attFile(messageAsAttachFile, mp, emailEncoding);

						msg.setContent(mp);
					} else {
						//mame iba textovy obsah, posli zjednodusene
						htmlDataNoBR = SearchTools.htmlToPlain(htmlDataNoBR);
						msg.setContent(htmlDataNoBR, "text/plain; charset="+emailEncoding);
					}

					String formMailFixedSenderEmail = Constants.getString("formMailFixedSenderEmail");
					if (Tools.isEmail(formMailFixedSenderEmail)) {
						msg.setFrom(new InternetAddress(formMailFixedSenderEmail, formMailFixedSenderEmail));
					} else {
						if (Tools.isEmail(Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY))) {
							from = Constants.getString(SendMail.EMAIL_PROTECTION_SENDER_KEY);
							Address[] oldSenders = msg.getFrom();
							if (oldSenders != null && oldSenders.length>0 && from.equals(oldSenders[0].toString()) == false) msg.setReplyTo(oldSenders);
							msg.setFrom(new InternetAddress(from, from));
						}
					}

					//ak mame parameter "replyTo", nastavme ho
					String replyTo = formSettings.getReplyTo();
					if(Tools.isEmail(replyTo)) {
						InternetAddress[] replyToAddrs = InternetAddress.parse(replyTo, false);
						msg.setReplyTo(replyToAddrs);
					}

					Transport.send(msg);

					sb.append(" succesfully send to email ").append(recipients);

					Adminlog.add(Adminlog.TYPE_SENDMAIL, "email from:" + from + " to:" + recipients + " subject:" + subject, (long)form.getDocId(), form.getId());
				}
				catch (Exception ex) {
					Logger.error(FormMailService.class, ex);
					sb.append(" sending to email ").append(recipients).append(" FAILED: ").append(ex.getMessage());
				}
			}
		} else {
			sb.append(" successfully saved");
		}

		// add send parameters
		sb.append("\n\n form parameters: \n");
		Map<String, String> formData = MultistepFormsService.getFormDataAsMap(form);
		formData.forEach((key, value) -> {
			if (value != null) value = value.replaceAll("\\n", "\\n    ");
			sb.append("  ").append(key).append(": ").append(value).append("\n");
		});

		sb.append("\n formName: ").append(form.getFormName());
		if (from != null) sb.append("\n from: ").append(from);
		if (Tools.isNotEmpty(recipients)) sb.append("\n to: ").append(recipients);
		if (Tools.isNotEmpty(recipients)) sb.append("\n subject: ").append(subject);
		sb.append("\n");

		Adminlog.add(Adminlog.TYPE_FORMMAIL,  sb.toString(), (long)form.getDocId(), form.getId());
    }
}
