<%@page pageEncoding="utf-8" isErrorPage="true"%><%@page import="sk.iway.iwcm.Identity"%><%@page import="sk.iway.iwcm.Constants"%><%@page import="java.io.PrintWriter"%><%@page import="java.io.StringWriter"%><%@
page import="sk.iway.iwcm.PathFilter" %><%@ page import="sk.iway.iwcm.i18n.Prop" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%
//toto mame kvoli penetracnym testom, aby nam nepindali na URL typu /Shortcut to FormMailAjax.action
response.setStatus(404);

//nastav security hlavicky
sk.iway.iwcm.SetCharacterEncodingFilter.setCommonHeaders(response, request);
%>

<iwcm:text key="stat.error.404"/>

<%
Identity user = sk.iway.iwcm.users.UsersDB.getCurrentUser(request);
if ((user != null && user.isAdmin()) || "iwcm.interway.sk".equals(request.getServerName()))
{
	response.setContentType("text/plain");
	if (exception!=null)
	{
		out.println(exception.getMessage());

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);

		pw.close();
		sw.close();

		out.println(sw.toString());
	}
	else
	{
		String path = PathFilter.getOrigPath(request);
		if (path.endsWith(".do"))
		{
			//je to struts chyba, asi priradenia tokenu
			out.println(Prop.getInstance(request).getText("components.csrfError"));
		}
	}
}
%>
