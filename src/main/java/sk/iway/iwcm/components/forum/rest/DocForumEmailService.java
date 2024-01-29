package sk.iway.iwcm.components.forum.rest;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.calendar.CalendarDB;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.components.forum.jpa.ForumGroupEntity;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.users.UsersDB;

/**
 * Handle sending notification email's abou forum.
 *
 * Email's -> Approving forum, New added forum, New added forumas response (answer) to existing forum
 */
public class DocForumEmailService {

	private Prop prop;
	private HttpServletRequest request;

	private DocForumEntity forumForm;
	private ForumGroupEntity forumGroup;
	private Integer forumFormDocId;

	private String header;
	private String footer;
	private String baseHref;

	private String fromNameGlobal;
	private String fromEmailGlobal;

	private String docTitle = "";
	private String docData = "";
	private String docUrl;

	//Inicialize if user is LOGGED and his email is SAME as sender email in forum form
	private boolean senderIsLogged = false;

	public DocForumEmailService(DocForumEntity forumForm, ForumGroupEntity forumGroup, HttpServletRequest request, Prop prop) {
		this.prop = prop;
		this.request = request;

		this.forumForm = forumForm;
		this.forumGroup = forumGroup;
		this.forumFormDocId = forumForm.getDocId();

		this.header = prop.getText("components.forum.hlavicka");
		this.footer = prop.getText("components.forum.paticka");
		this.baseHref = Tools.getBaseHref(request);

		prepareSupportValues();
	}

	/**
	 * Prepare all the variables that are necessary to send forum email's.
	 */
	private void prepareSupportValues() {
		//
		Identity user = UsersDB.getCurrentUser(request);
		if(user != null && forumForm != null && forumForm.getAuthorEmail().equals(user.getEmail())) senderIsLogged = true;

		if(forumGroup == null) {
			forumGroup = new ForumGroupEntity();
			forumGroup.setMessageConfirmation(false);
		}

		fromNameGlobal = Constants.getString("forumNotifySenderName");
		if(Tools.isEmpty(fromNameGlobal)) fromNameGlobal = forumForm.getAuthorName();

		fromEmailGlobal = Constants.getString("forumNotifySenderEmail");
		if (Tools.isEmpty(fromEmailGlobal)) fromEmailGlobal = forumForm.getAuthorEmail();
		if (Tools.isEmpty(fromEmailGlobal)) fromEmailGlobal = "info@" + Tools.getServerName(request);


		DocDB docDB = DocDB.getInstance();
		DocDetails doc = docDB.getDoc(forumFormDocId);
		docUrl = "/showdoc.do?docid=" + forumFormDocId;
		if (doc != null) {
			docTitle = doc.getTitle();
			docData = doc.getData();
			if (Tools.isNotEmpty(doc.getVirtualPath())) docUrl = doc.getVirtualPath();
		}
	}

	/**
	 * Send approve request email.
	 * Body of email (message) will be generated.
	 *
	 * @param fromEmail
	 * @param toEmail
	 */
    private void sendApproveEmail(String fromEmail, String toEmail) {
		String approveLink = baseHref + "/apps/forum/admin/#dt-filter-id=" + forumForm.getId() + "&dt-select=true";

		StringBuilder message = new StringBuilder(header);
			message.append("<p>");
				message.append(prop.getText("components.forum.approve.text")).append(".<br/>");
				message.append("DocID: ").append(forumFormDocId);
			message.append("</p>");

			message.append("<table border='0' cellpadding='0' cellspacing='0'>");
				message.append("<tr><td style='vertical-align: top;'>").append(prop.getText("components.forum.message_name")).append(": </td><td>").append(docTitle).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.author")).append(": </td><td>").append(fromNameGlobal).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.message_text")).append(": </td><td>").append(" " + forumForm.getQuestion()).append("</td></tr>");
			message.append("</table>");

			message.append("<p>");
				message.append(prop.getText("components.forum.message_approve_link")).append(": <a href='").append(approveLink).append("'>").append(approveLink).append("</a><br/>");
			message.append("</a></p>");
		message.append(footer);

		SendMail.send(fromNameGlobal, fromEmail, toEmail, prop.getText("components.forum.approve.subject"), message.toString());
	}

	/**
	 * Send notification email, that inform about new added forum.
	 * Body of email (message) will be generated.
	 *
	 * @param fromEmail
	 * @param toEmail
	 * @param messageLink - link taht will be included in email body
	 */
	private void sendNotificationEmail(String fromEmail, String toEmail, String messageLink) {
		StringBuilder message = new StringBuilder(header);
			message.append("<p>");
				message.append(prop.getText("components.forum.email_subject")).append(".<br/>");
			message.append("</p>");

			message.append("<table border='0' cellpadding='0' cellspacing='0'>");
				message.append("<tr><td style='vertical-align: top;'>").append(prop.getText("components.forum.message_name")).append(": </td><td>").append(docTitle).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.author")).append(": </td><td>").append(fromNameGlobal).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.message_text")).append(": </td><td>").append(" " + forumForm.getQuestion()).append("</td></tr>");
			message.append("</table>");

			message.append("<p>").append(prop.getText("components.forum.open_forum")).append(": <br/><a href='").append(messageLink).append("'>").append(messageLink).append("</a></p>");
		message.append(footer);

		SendMail.send(fromNameGlobal, fromEmail, toEmail, prop.getText("components.forum.email_subject"), message.toString());
	}

	/**
	 * Send notification email, abou ned added forum, that is response to allready existing forum.
	 * Body of email (message) will be generated.
	 *
	 * @param fromEmail
	 * @param toEmail
	 * @param messageLink - link taht will be included in email body
	 * @param docForumEntity
	 */
	private void sendNotificationAnswerEmail(String fromEmail, String toEmail, String messageLink, DocForumEntity docForumEntity) {
		StringBuilder message = new StringBuilder(header);
			message.append("<p>").append(prop.getText("components.forum.email_answer_subject")).append(".</p>");

			message.append("<table border='0' cellpadding='0' cellspacing='0'>");
				message.append("<tr><td style='vertical-align: top;'>").append(prop.getText("components.forum.answer_message_name")).append(": </td><td>").append(docForumEntity.getSubject()).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.email_your_question")).append(": </td><td>").append(docForumEntity.getQuestion()).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.email_answer")).append(": </td><td>").append(forumForm.getSubject()).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.answer_author")).append(": </td><td>").append(fromNameGlobal).append("</td></tr>");
				message.append("<tr><td>").append(prop.getText("components.forum.answer_text")).append(": </td><td>").append(forumForm.getQuestion()).append("</td></tr>");
			message.append("</table>");

			message.append("<p>").append(prop.getText("components.forum.open_forum")).append(": <br/><a href='").append(messageLink).append("'>").append(messageLink).append("</a></p>");
		message.append(footer);

		SendMail.send(fromNameGlobal, fromEmail, toEmail, prop.getText("components.forum.email_answer_subject"), message.toString());
	}

	/**
	 * Send notification email about new added forum that can be answer to another forum.
	 * Body of email is NOT generated but retrieved as translation text.
	 *
	 * @param fromEmail
	 * @param toEmail
	 * @param messageKey
	 * @param messageLink
	 */
	private void sendEmailByMessageKey(String fromEmail, String toEmail, String messageKey, String messageLink) {
		String message = prop.getText(messageKey, messageLink, docTitle, docData, fromEmailGlobal, forumForm.getQuestion());
		SendMail.send(fromNameGlobal, fromEmail, toEmail, prop.getText("components.forum.email_answer_subject"), message);
	}

	/**
	 * Return ForumId of root parent.
	 *
	 * @return
	 */
	private int getRootParentForumId() {
		int rootForumId = forumForm.getId().intValue();
		int parentId = forumForm.getParentId();

		while(true) {
			if(parentId < 1) return rootForumId;
			else rootForumId = parentId;

			parentId = (new SimpleQuery()).forInt("SELECT parent_id FROM document_forum WHERE forum_id=?", rootForumId);
		}
	}


	/**
	 * By defined logic send required email's about forum.
	 * IF forum need's to be required -> send only approving email.
	 * IF forum does not need to be required -> send notif email's based on another params (more details in this fn).
	 */
	public void sendForumEmails() {
		//Check if forummust be approved first
		if(forumGroup.getMessageConfirmation() && Tools.isEmail(forumGroup.getApproveEmail()) && !forumForm.getConfirmed()) {
			//FORUM is not Approved, send approving request email
			sendApproveEmail(fromEmailGlobal, forumGroup.getApproveEmail());
		} else  {
			//Forum is approved or do not need to be approved

			//Prepare message link
			String messageLink = "";
			if(forumFormDocId > CalendarDB.FORUM_OFFSET) {
				String calendarUrl = prop.getText("components.calendar.url", String.valueOf(forumFormDocId - CalendarDB.FORUM_OFFSET));
				messageLink = baseHref + calendarUrl;
			} else {
				messageLink = baseHref + Tools.addParameterToUrl(docUrl, "pId", "" + getRootParentForumId()); //To open right forum
				messageLink += "#post" + forumForm.getId(); //To scroll on this answer
			}

			//moznost do request atributu zadat kluc, ktory sa pouzije na text spravy. Napr. na nastenke chceme mat iny text ako v diskusii na stranke
			String messageKey = null;
			if (request.getAttribute("forumNotifyMessageKey") != null) {
				String messageKeyVerify = (String)request.getAttribute("forumNotifyMessageKey");
				String messageText = prop.getText(messageKeyVerify);

				if (messageKeyVerify.equals(messageText) == false) messageKey = messageKeyVerify;
			}

			//NOTIFICATION EMAIL - NEW ADDED COMENT to forum (IT'S ANSWERS to another forum comment)
			boolean answerEmailWasSend = false;
			String toParentEmail = "";
			if(forumForm.getParentId() > 0) {
				DocForumEntity docForumEntity = DocForumService.getForumBean(forumForm.getParentId().intValue(), true);
				if (docForumEntity != null && docForumEntity.isSendNotif() && Tools.isEmail(docForumEntity.getAuthorEmail())) {
					toParentEmail = docForumEntity.getAuthorEmail();
					//If FROM email and TO email are same, do not send email -> we dont need inform user who answer to his own commnet
					if(senderIsLogged && !fromEmailGlobal.equals(toParentEmail)) {
						if (messageKey != null)
							sendEmailByMessageKey(fromEmailGlobal, toParentEmail, messageKey, messageLink);
						else
							sendNotificationAnswerEmail(fromEmailGlobal, toParentEmail, messageLink, docForumEntity);

						answerEmailWasSend = true;
					}
				}
			}

			//NOTIFICATION EMAIL - NEW ADDED COMENT to forum
			String toNotifyEmail = forumGroup.getNotifEmail();
			//If answer email was send AND receiver email is same as notifycation email DO NOT send notification email (it would be excesive)
			if(Tools.isEmail(toNotifyEmail) && !( answerEmailWasSend && toNotifyEmail.equals(toParentEmail))) {
				//If FROM email and TO email are same, do not send email -> we dont need inform user who answer to topic
				if(senderIsLogged && !fromEmailGlobal.equals(toNotifyEmail)) {
					if (messageKey != null)
						sendEmailByMessageKey(fromEmailGlobal, toNotifyEmail, messageKey, messageLink);
					else
						sendNotificationEmail(fromEmailGlobal, toNotifyEmail, messageLink);
				}
			}
		}
	}
}