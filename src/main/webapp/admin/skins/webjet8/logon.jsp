<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

if (Constants.getBoolean("adminRequireSSL") && Tools.isSecure(request) == false)
{
	String path = PathFilter.getOrigPath(request);
	String qs = Tools.getRequestQueryString(request);
	StringBuilder url = new StringBuilder("https://").append(request.getServerName()).append(path);
	if (Tools.isNotEmpty(qs)) url.append('?').append(qs);
	response.sendRedirect(Tools.sanitizeHttpHeaderParam(url.toString()));
	return;
}

//--------------Prihlasenie prebieha cez NTLM, nema sa co prihlasovat cez formularik-----------
if (AuthenticationFilter.weTrustIIS())
{
	response.sendRedirect( AuthenticationFilter.getForbiddenURL() );
	return;
}

if  (Tools.getRequestParameter(request, "language")==null && request.getSession().getAttribute(Prop.SESSION_I18N_PROP_LNG)==null && Tools.isNotEmpty(Constants.getString("defaultLanguage")))
{
	request.getSession().setAttribute(Prop.SESSION_I18N_PROP_LNG, Constants.getString("defaultLanguage"));
}

response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

String adminHost = Constants.getString("multiDomainAdminHost");
//out.println("adminHost="+adminHost+" domain="+DocDB.getDomain(request));

String serverName = request.getServerName();
String forwardHost = request.getHeader("x-forwarded-host");
if (Constants.getBoolean("serverBeyoundProxy") && Tools.isNotEmpty(forwardHost)) serverName = forwardHost;

if ("web.vucke.sk".equals(serverName))
{
	String hosts[] = Tools.getTokens(adminHost, ",");
	response.sendRedirect(request.getScheme()+"://"+hosts[0]+"/admin/");
	return;
}

if (("iwcm.interway.sk".equals(request.getServerName())==false && "localhost".equals(request.getServerName())==false) && Tools.isNotEmpty(adminHost) && (","+adminHost+",").indexOf(","+serverName+",")==-1)
{
	if (Constants.getBoolean("adminLogonShowSimpleErrorMessage"))
	{
		pageContext.include("/404.jsp");
		return;
	}
	else
	{
		String hosts[] = Tools.getTokens(adminHost, ",");
		response.sendRedirect(request.getScheme()+"://"+hosts[0]+"/admin/");
		return;
	}
}

LogonTools.saveAfterLogonRedirect(request);

//nova spring verzia
response.sendRedirect("/admin/logon/");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*"
%><%@ page import="sk.iway.iwcm.system.ntlm.AuthenticationFilter" %><%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.common.LogonTools" %>
