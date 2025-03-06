<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

String forward = "/403-"+Constants.getInstallName()+".jsp";
java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
if (fForward.exists())
{
	pageContext.forward(forward);
	return;
}

//nastav security hlavicky
sk.iway.iwcm.SetCharacterEncodingFilter.setCommonHeaders(response, request);

%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.stat.*,sk.iway.iwcm.*"
%><%@ page import="sk.iway.iwcm.system.UrlRedirectDB,sk.iway.iwcm.doc.*" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%


	String path = (String)request.getAttribute("path_filter_orig_path");
	//presmerovanie admin komponent
	if (path!=null && path.startsWith("/components"))
	{
		if (path.contains("admin"))
		{
			if (PathFilter.checkAdmin(request))
			{
				Identity user = sk.iway.iwcm.users.UsersDB.getCurrentUser(request);
				if (user == null)
				{
                    LogonTools.saveAfterLogonRedirect(request);
					//nemame usera, asi sme admin cast, presmeruj
					response.sendRedirect("/admin/logon/");
					return;
				}
			}
		}
	}

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

	if (path!=null && path.contains("/rest/")) {
		return;
	}

%>


<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.system.multidomain.MultiDomainFilter"%>
<%@ page import="sk.iway.iwcm.common.LogonTools" %>
<html>
	<head>
		<title>403</title>
		<meta http-equiv="Content-type" content="text/html;charset=windows-1250">
	</head>
	<body>
	<!--
	-- Unnnfortunately, Microsoft has added a clever new
	-- "feature" to Internet Explorer. If the text in
	-- an error's message is "too small", specifically
	-- less than 512 bytes, Internet Explorer returns
	-- its own error message. Yes, you can turn that
	-- off, but *surprise* it's pretty tricky to find
	-- buried as a switch called "smart error
	-- messages" That means, of course, that many of
	-- error messages are censored by default.
	-- And, of course, you'll be shocked to learn that
	-- IIS always returns error messages that are long
	-- enough to make Internet Explorer happy. The
	-- workaround is pretty simple: pad the error
	-- message with a big comment to push it over the
	-- five hundred and twelve byte minimum. Of course,
	-- that's exactly what you're reading right now....
	-->

	<center>
	   <br><br>
		 <iwcm:text key="stat.error.404"/>
	</center>
	</body>
</html>
