<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%

int userId = Tools.getIntValue(request.getParameter("userId"), -1);
String hash = request.getParameter("hash");

if (userId > 0 && Tools.isNotEmpty(hash))
{
	boolean authorizeOK = UsersDB.checkHast(userId, hash);
	if (authorizeOK)
	{
		//posli mu info email
		AuthorizeAction.sendInfoEmail(userId, null, request);
		request.setAttribute("authorizeOK", "true");
	}
}
else if (userId == -1 && Tools.isNotEmpty(hash))
{
	//autorizacia dmailu
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), lng, false);
	System.out.println("mam hash: " + hash);
	UserDetails authorizedUser = UsersDB.authorizeEmail(request, hash);
	if (authorizedUser == null)
	{
		//zadany hash neexistuje, alebo nastala ina chyba
		%><iwcm:text key="dmail.subscribe.error_authorize_email"/><%
	}
	else
	{
		//podarilo sa nam ho nacitat
		%><iwcm:text key="dmail.subscribe.email_authorized"/><%
	}
	return;
}
%>

<%@page import="sk.iway.iwcm.i18n.Prop"%><logic:notPresent name="authorizeOK">
   <iwcm:text key="user.authorize.fail"/>
</logic:notPresent>
<logic:present name="authorizeOK">
   <iwcm:text key="user.authorize.success"/>
</logic:present>