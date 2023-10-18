<%@page import="java.util.Set"%><%@page import="java.util.HashSet"%><%@page import="sk.iway.iwcm.doc.TemplatesDB"%><%@page import="sk.iway.iwcm.database.SimpleQuery"%><%@page import="sk.iway.iwcm.doc.DocDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/plain");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!

private static Set<String> allowedIps = new HashSet<String>();

private boolean isIpAllowed(String remoteIp)
{
	if (allowedIps == null) allowedIps = new HashSet<String>();
	if (allowedIps.contains(remoteIp)) return true;

	String[] ips = Tools.getTokens(Constants.getString("serverMonitoringEnableIPs"), ",");
	for (String ip : ips)
	{
		if (remoteIp.startsWith(ip))
		{
			allowedIps.add(remoteIp);
			return true;
		}
	}

	return false;
}


%><%

if (isIpAllowed(Tools.getRemoteIP(request))==false)
{
	System.out.println("monitor.jsp, forbidden for IP "+Tools.getRemoteIP(request));

	response.setStatus(403);
	pageContext.include("/403.jsp");
	return;
}

response.setDateHeader("Expires",0);
response.setDateHeader("Last-Modified", Tools.getNow());
response.setHeader("Cache-Control","no-store, no-cache, must-revalidate, max-age=0");
response.setHeader("Pragma","No-Cache");
response.setHeader("Etag", "");

//monitorovacia JSP pre dohladovy SW, vrati text "OK" a HTTP status 200 ak je vsetko OK, inak vrati text chyby a HTTP status 500

String errorMessage = null;

try
{
	if (InitServlet.isWebjetInitialized()==false)
	{
		errorMessage = "NOT INITIALISED";
	}

	if (errorMessage==null && Tools.getRequestParameter(request, "nagios")==null)
	{
		//nagios parameter sa pouziva pre dohlad, aby mu to nehlasilo po starte ze nejde, ale clustru ze este nema zaradit

		//pockaj 3 minuty po starte na zaradenie do clustra
		if (InitServlet.getServerStartDatetime()==null || (InitServlet.getServerStartDatetime().getTime()+(Constants.getInt("monitoringPreheatTime")*1000)) > Tools.getNow())
		{
			errorMessage = "TOO SHORT AFTER START";
		}
	}

	if (errorMessage==null)
	{
		//otestuj DB query
		int monitorTestDocId = Constants.getInt("monitorTestDocId");
		if (monitorTestDocId == -1) monitorTestDocId = 1;
		if (monitorTestDocId > 0)
		{
			String title = new SimpleQuery().forString("SELECT title FROM documents WHERE doc_id="+monitorTestDocId);
			if (Tools.isEmpty(title))
			{
				errorMessage = "DEFAULT DOC NOT FOUND";
			}
		}
	}

	if (errorMessage == null)
	{
		TemplatesDB temps = TemplatesDB.getInstance();
		if (temps == null || temps.getTemplates().size() < 3)
		{
			errorMessage = "NOT ENOUGHT TEMPLATES";
		}
	}

	if (errorMessage == null)
	{
		if (sk.iway.iwcm.stat.StatWriteBuffer.size() > Constants.getInt("statBufferSuspicionThreshold"))
		{
			errorMessage = "STAT BUFFER SUSPICION";
		}
	}

    if (errorMessage == null)
    {
        if (Constants.getBoolean("monitorMaintenanceMode"))
        {
            errorMessage = "UNAVAILABLE";
        }
    }
}
catch (Exception ex)
{
	errorMessage = "EXCEPTION: "+ex.getMessage();
}

if (errorMessage == null)
{
	out.println("OK");
	return;
}

System.out.println("monitor.jsp, errorMessage="+errorMessage);

response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
out.print(errorMessage);
%>
