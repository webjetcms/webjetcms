<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.*,sk.iway.iwcm.tags.JSEscapeTag"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@page import="sk.iway.iwcm.dmail.EmailDB"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@page import="sk.iway.iwcm.dmail.Sender"%>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), lng, false);

	// odhlasenie z mailing listu
	PageParams pageParams = new PageParams(request);
	boolean confirmUnsubscribe = pageParams.getBooleanValue("confirmUnsubscribe", false);
	String confirmUnsubscribeText = pageParams.getValue("confirmUnsubscribeText", "");
	if (Tools.isNotEmpty("confirmUnsubscribeText") && confirmUnsubscribeText.length() > 8)
	{
		request.setAttribute("confirmUnsubscribeText", confirmUnsubscribeText);
	}

	UserGroupsDB userGroupsDB = UserGroupsDB.getInstance(sk.iway.iwcm.Constants.getServletContext(), false, DBPool.getDBName(request));
	List userGroupsList = userGroupsDB.getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL);
	request.setAttribute("userGroupsList", userGroupsList);

	String email = request.getParameter("email");

	//skus najst prihlaseneho pouzivatela
	Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
	if (user != null)
	{
		System.out.println("mam usera...");
		if (email == null)
			email = user.getEmail();
	}

	if (email == null)
		email = "";

	int dmspID = sk.iway.iwcm.dmail.Sender.getEmailIdFromClickHash(request.getParameter(Constants.getString("dmailStatParam")));
	if (Tools.isNotEmpty(email) && ( (dmspID > 0 && confirmUnsubscribe==false) || ("true".equals(request.getParameter("save"))) ) )
	{
		String emailDmsp = EmailDB.getEmail(dmspID);

		boolean saveOK = false;
		if (dmspID < 0) {
			//direct entry of email into form - send conformation email to user with link to unsubscribe
			String baseHref = Constants.getString("dmailListUnsubscribeBaseHref", Tools.getBaseHref(request));
			String unsubscribedUrl = Tools.addParameterToUrl(PathFilter.getOrigPathDocId(request), "email", email);
			//skip confirmation on click (user just added email to unsubscribe so do not ask for confirmation again)
			unsubscribedUrl = Tools.addParameterToUrl(unsubscribedUrl, "save", "true");

			String subject = prop.getText("dmail.subscribe.subject");
			String message = prop.getText("dmail.unsubscribe.bodyNew", unsubscribedUrl);

			boolean ok = SendMail.sendLater(SendMail.getDefaultSenderName("dmail", email), SendMail.getDefaultSenderEmail("dmail", email), email, null, null, null, subject, message, baseHref, null, null);
			if (ok) {
				request.setAttribute("unsubscribeSuccess-showEmailSent", "true");
				request.removeAttribute("confirmUnsubscribeText");
			} else {
				request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.error_unsubscribe_email"));
			}
		} else if (email.equalsIgnoreCase(emailDmsp)) {
			saveOK = EmailDB.addUnsubscribedEmail(email);
			if (saveOK) {
				request.setAttribute("unsubscribeSuccess", prop.getText("dmail.unsubscribe.emailunsubscribed", email));
				if (dmspID > 0) {
					request.setAttribute("unsubscribeSuccess-showUndelete", "true");
				}
			} else {
				request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.error_unsubscribe_email"));
			}
		} else {
			request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.email_not_match"));
		}
	}

	//undelete is possible only when there is matching dmspID email
	if (Tools.isEmail(email) && dmspID>0 && "true".equals(request.getParameter("undelete")))
	{
		String emailDmsp = EmailDB.getEmail(dmspID);
		if (email.equalsIgnoreCase(emailDmsp)) {
			boolean undeleted = sk.iway.iwcm.common.EmailToolsForCore.deleteUnsubscribedEmail(email);
			if (undeleted) {
				request.setAttribute("unsubscribeSuccess", prop.getText("components.dmail.unsubscribe.emailunsubscribed", email));
			} else {
				request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.error_unsubscribe_email"));
			}
		} else {
			request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.email_not_match"));
		}
	}

	//prislo potvrdenie zmeny = backward kompatibilita
	if (request.getParameter("hash")!=null)
	{
		String hash = request.getParameter("hash");
		System.out.println("mam hash: " + hash);
		UserDetails authorizedUser = UsersDB.authorizeEmail(request, hash);
		if (authorizedUser == null)
		{
			//zadany hash neexistuje, alebo nastala ina chyba
			out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.subscribe.error_authorize_email")+".\");</script>");
		}
		else
		{
			//podarilo sa nam ho nacitat
			email = authorizedUser.getEmail();
			out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.subscribe.email_authorized")+".\");</script>");
		}
	}
%>

<div class="unsubscribeForm">
	<form action="<%=Tools.replace(PathFilter.getOrigPathDocId(request), "email=", "em=") %>" method="post">
		<fieldset>
			<iwcm:present name="unsubscribeSuccess-showEmailSent">
				<div class="alert alert-info" role="alert">
					<iwcm:text key="dmail.unsubscribe.confirm_email_sent"/>
				</div>
			</iwcm:present>

			<iwcm:present name="confirmUnsubscribeText">
				<div class="unsubscribe-confirm-text">
					<iway:request name="confirmUnsubscribeText"/>
					<iwcm:notPresent name="unsubscribeSuccess">
						<p>
							<a href="/" class="btn btn-primary"><iwcm:text key="components.dmail.unsubscribe.unsubscribeCancel"/></a>
						</p>
					</iwcm:notPresent>
				</div>
			</iwcm:present>

			<iwcm:present name="unsubscribeErrors">
				<div class="alert alert-danger" role="alert">
					<strong><iwcm:text key="dmail.subscribe.errors"/>:</strong>
					<ul>
						<li><iway:request name="unsubscribeErrors"/></li>
					</ul>
				</div>
			</iwcm:present>

			<iwcm:present name="unsubscribeSuccess">
				<div class="alert alert-success" role="alert">
					<iway:request name="unsubscribeSuccess"/>
				</div>
			</iwcm:present>

			<iwcm:notPresent name="unsubscribeSuccess">
				<p class="email-address">
					<label for="unsubscribeEmail" class="form-label"><iwcm:text key="dmail.subscribe.email"/>:</label>
					<input id="unsubscribeEmail" class="emailField form-control" type="text" name="email" value="<%=ResponseUtils.filter(email)%>" <% if (dmspID>0 && Tools.isNotEmpty(email)) out.print("readonly='readonly'"); %> />
				</p>
				<p>
					<input type="hidden" name="save" value="true" />
					<input type="submit" class="bSubmit btn btn-secondary" name="bSubmit" value="<iwcm:text key="dmail.unsubscribe.unsubscribe"/>" />
				</p>
			</iwcm:notPresent>

			<iwcm:present name="unsubscribeSuccess-showUndelete">
				<p>
					<iwcm:text key="components.dmail.unsubscribe.unsubscribeUndelete.text"/>
				</p>
				<p>
					<input type="hidden" name="undelete" value="true" />
					<input type="hidden" name="email" value="<%=ResponseUtils.filter(email)%>"/>
					<input type="submit" class="bSubmit btn btn-primary" name="bSubmit" value="<iwcm:text key="components.dmail.unsubscribe.unsubscribeUndelete"/>" />
				</p>
			</iwcm:present>

		</fieldset>
		<input type="hidden" name="<%=Constants.getString("dmailStatParam") %>" value="<%=ResponseUtils.filter(request.getParameter(Constants.getString("dmailStatParam"))) %>" />
	</form>
</div>