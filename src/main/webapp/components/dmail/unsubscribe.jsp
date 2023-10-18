<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*,sk.iway.iwcm.users.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.*,sk.iway.iwcm.tags.JSEscapeTag"%>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.dmail.EmailDB"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), lng, false);

	//Registracia do mailing listu, umozni zadat meno, email a skupiny emailov, ktore chce dostavat
	PageParams pageParams = new PageParams(request);

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
	if (dmspID > 0 || ("true".equals(request.getParameter("save")) && Tools.isNotEmpty(email)))
	{
		String emailDmsp = EmailDB.getEmail(dmspID);

		boolean saveOK = false;
		if (email.equalsIgnoreCase(emailDmsp) || (dmspID < 0))
		{
			saveOK = EmailDB.addUnsubscribedEmail(email);
			if (saveOK)
				out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.unsubscribe.emailunsubscribed")+"\");</script>");
			else
				out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.unsubscribe.error_unsubscribe_email")+"\");</script>");
		}
		else
			out.println("<script type='text/javascript'> window.alert(\""+prop.getText("dmail.unsubscribe.email_not_match")+"\");</script>");
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
	<logic:present name="errors">
		<b><iwcm:text key="dmail.subscribe.errors"/>:</b>
		<ul style="color: red">
			<iway:request name="errors"/>
		</ul>
	</logic:present>

	<form action="<%=Tools.replace(PathFilter.getOrigPathDocId(request), "email=", "em=") %>" method="post">
		<fieldset>
			<p>
				<label>
					<strong><iwcm:text key="dmail.subscribe.email"/>:</strong>
					<input class="emailField" type="text" name="email" value="<%=ResponseUtils.filter(email)%>" id="mail" />
				</label>
			</p>

			<p>
				<label>&nbsp;</label>
				<input type="hidden" name="save" value="true" />
				<input type="hidden" name="<%=Constants.getString("dmailStatParam") %>" value="<%=ResponseUtils.filter(request.getParameter(Constants.getString("dmailStatParam"))) %>" />
				<input type="submit" class="bSubmit" name="bSubmit" value="<iwcm:text key="dmail.unsubscribe.unsubscribe"/>" />
			</p>
		</fieldset>
	</form>
</div>