<%@page import="java.util.List"%>
<%@page import="java.util.Base64"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.*,sk.iway.iwcm.tags.JSEscapeTag"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.dmail.EmailDB"%>
<%@page import="sk.iway.iwcm.dmail.Sender"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), lng, false);

	// Registracia do mailing listu
	PageParams pageParams = new PageParams(request);
	boolean confirmUnsubscribe = pageParams.getBooleanValue("confirmUnsubscribe", false);
	String confirmUnsubscribeText = pageParams.getValue("confirmUnsubscribeText", "");

	if (Tools.isNotEmpty("confirmUnsubscribeText") && confirmUnsubscribeText.length() > 8) {
		request.setAttribute("confirmUnsubscribeText", confirmUnsubscribeText);
	}

	UserGroupsDB userGroupsDB = UserGroupsDB.getInstance(sk.iway.iwcm.Constants.getServletContext(), false, DBPool.getDBName(request));
	List userGroupsList = userGroupsDB.getUserGroupsByTypeId(UserGroupDetails.TYPE_EMAIL);
	request.setAttribute("userGroupsList", userGroupsList);

	String email = request.getParameter("email");

	// Zisti email prihlaseného používateľa
	Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
	if (user != null && Tools.isEmpty(email)) {
		System.out.println("mam usera...");
		email = user.getEmail();
	}
	email = Tools.isEmpty(email) ? "" : email;

	// Skús získať email ID z parametra
	int dmspID = sk.iway.iwcm.dmail.Sender.getEmailIdFromClickHash(request.getParameter(Constants.getString("dmailStatParam")));

	// Unsubscribe z mailingu
	if (dmspID > 0) {
		String emailDmsp = EmailDB.getEmail(dmspID);
		boolean saveOK = EmailDB.addUnsubscribedEmail(emailDmsp);

		if (saveOK) {
			request.setAttribute("unsubscribeSuccess", prop.getText("dmail.unsubscribe.emailunsubscribed", emailDmsp));
			request.setAttribute("unsubscribeSuccess-showUndelete", "true");

		} else {
			request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.error_unsubscribe_email"));
		}
	}

	// Odoslanie overovacieho mailu pre odhlásenie
	boolean saveRequested = "true".equals(request.getParameter("save"));
	if (Tools.isNotEmpty(email) && dmspID < 0 && saveRequested) {
        String baseDomain = Tools.getBaseHrefLoopback(request);
		String baseHref = Constants.getString("dmailListUnsubscribeBaseHref", baseDomain );
		String safeHref = Tools.replace(baseHref, "http://", "https://");


		Integer emailId = EmailDB.getEmailId(email);
        if (emailId != null) {
            String hash = Sender.getClickHash(emailId);
		    String unsubscribedUrl = safeHref + "/newsletter/odhlasenie-z-newsletra.html?" + Constants.getString("dmailStatParam") + "=" + hash;

		    String fromName = "WebjetCMS";
		    String fromEmail = "tester@balat.sk";
		    String toEmail = email;
		    String subject = "Overenie mailu pre odhlásenie";
		    String message = prop.getText("dmail.unsubscribe.bodyNew", unsubscribedUrl);
		    String serverRoot = "http://" + Tools.getServerName(request);

		    boolean ok = SendMail.sendLater(fromName, fromEmail, toEmail, null, null, null, subject, message, serverRoot, null, null);
            request.setAttribute("pageSend", ok ? "ok" : "fail");
        }


		request.setAttribute("unsubscribeSuccess-showEmailSent", "true");
	}

	// Opätovné prihlásenie emailu
	String emailDmsp = EmailDB.getEmail(dmspID);
	if (Tools.isEmail(emailDmsp) && dmspID > 0 && "true".equals(request.getParameter("undelete"))) {
		boolean undeleted = sk.iway.iwcm.common.EmailToolsForCore.deleteUnsubscribedEmail(emailDmsp);

		if (undeleted) {
			request.setAttribute("unsubscribeSuccess", prop.getText("components.dmail.unsubscribe.emailunsubscribed", emailDmsp));
		} else {
			request.setAttribute("unsubscribeErrors", prop.getText("dmail.unsubscribe.error_unsubscribe_email"));
		}
	}

	// Potvrdenie emailu cez hash
	String hash = request.getParameter("hash");
	if (hash != null) {
		System.out.println("mam hash: " + hash);
		UserDetails authorizedUser = UsersDB.authorizeEmail(request, hash);

		if (authorizedUser == null) {
			out.println("<script type='text/javascript'> window.alert(\"" + prop.getText("dmail.subscribe.error_authorize_email") + ".\");</script>");
		} else {
			email = authorizedUser.getEmail();
			out.println("<script type='text/javascript'> window.alert(\"" + prop.getText("dmail.subscribe.email_authorized") + ".\");</script>");
		}
	}
%>

<div class="unsubscribeForm">
	<form action="<%=Tools.replace(PathFilter.getOrigPathDocId(request), "email=", "em=") %>" method="post">
		<fieldset>
			<logic:present name="confirmUnsubscribeText">
				<div class="unsubscribe-confirm-text">
					<iway:request name="confirmUnsubscribeText"/>
					<logic:notPresent name="unsubscribeSuccess">
						<p>
							<a href="/" class="btn btn-primary"><iwcm:text key="components.dmail.unsubscribe.unsubscribeCancel"/></a>
						</p>
					</logic:notPresent>
				</div>
			</logic:present>

			<logic:present name="unsubscribeErrors">
				<div class="alert alert-danger" role="alert">
					<strong><iwcm:text key="dmail.subscribe.errors"/>:</strong>
					<ul>
						<li><iway:request name="unsubscribeErrors"/></li>
					</ul>
				</div>
			</logic:present>

			<logic:present name="unsubscribeSuccess">
				<div class="alert alert-success" role="alert">
					<iway:request name="unsubscribeSuccess"/>
				</div>
			</logic:present>

			<logic:notPresent name="unsubscribeSuccess">
				<p class="email-address">
					<label for="unsubscribeEmail" class="form-label"><iwcm:text key="dmail.subscribe.email"/>:</label>
					<input id="unsubscribeEmail" class="emailField form-control" type="text" name="email" value="<%=ResponseUtils.filter(email)%>" <% if (dmspID>0 && Tools.isNotEmpty(email)) out.print("readonly='readonly'"); %> />
				</p>
				<p>
					<input type="hidden" name="save" value="true" />
					<input type="submit" class="bSubmit btn btn-secondary" name="bSubmit" value="<iwcm:text key="dmail.unsubscribe.unsubscribe"/>" />
				</p>
			</logic:notPresent>

			<logic:present name="unsubscribeSuccess-showUndelete">
				<p>
					<iwcm:text key="components.dmail.unsubscribe.unsubscribeUndelete.text"/>
				</p>
				<p>
					<input type="hidden" name="undelete" value="true" />
					<input type="hidden" name="email" value="<%=ResponseUtils.filter(email)%>"/>
					<input type="submit" class="bSubmit btn btn-primary" name="bSubmit" value="<iwcm:text key="components.dmail.unsubscribe.unsubscribeUndelete"/>" />
				</p>
			</logic:present>

			<logic:present name="unsubscribeSuccess-showEmailSent">
				<p>
					<iwcm:text key="dmail.unsubscribe.confirm_email_sent"/>
				</p>
			</logic:present>

		</fieldset>
		<input type="hidden" name="<%=Constants.getString("dmailStatParam") %>" value="<%=ResponseUtils.filter(request.getParameter(Constants.getString("dmailStatParam"))) %>" />
	</form>
</div>