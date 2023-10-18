<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/><%!

private void checkDir(String url, JspWriter out, String baseHref) throws IOException
{
	IwcmFile[] files = new IwcmFile(Tools.getRealPath(url)).listFiles();
	for (IwcmFile f : files)
	{
		if (f.isDirectory())
		{
			checkDir(url+f.getName()+"/", out, baseHref);
		}
		else if (f.getName().endsWith(".jsp"))
		{
			String fullUrl = url+f.getName();
			String htmlCode = Tools.downloadUrl(baseHref+fullUrl);
			if (htmlCode != null && htmlCode.indexOf("<title>403</title>")==-1)
			{
				out.println(Tools.escapeHtml(fullUrl)+"<br/>");
			}
			else if (fullUrl.indexOf("admin")==-1)
			{
				htmlCode = FileTools.readFileContent(fullUrl);
				if (htmlCode.indexOf("</html>")!=-1 || htmlCode.indexOf("top-public.jsp")!=-1)
				{
					out.println("<span style='color: red;'>"+Tools.escapeHtml(fullUrl)+"</span><br/>");
				}
			}
		}
	}
}


%>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Komponenta pre overenie povolenia pristupu k JSP suborom z /components/ adresara, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>
<p>
Subory oznacene cervenou farbou je potrebne skontrolovat a pripadne pridat do konfiguracnej premennej componentsDirectCallExceptions.
</p>
<%
if ("fix".equals(request.getParameter("act"))) {
	checkDir("/components/", out, Tools.getBaseHref(request));
}
%>

<%@ include file="/admin/layout_bottom.jsp" %>
