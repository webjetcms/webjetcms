<%@page import="java.io.IOException"%>
<%@page import="java.io.File"%>
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

public void checkWritableFolders(String url, JspWriter out) throws IOException
{
	File dir = new File(Tools.getRealPath(url));
	if (dir == null) return;
	File files[] = dir.listFiles();
	if (files == null) return;

	for (File f : files)
	{
		if (f.isDirectory())
		{
			if (f.canWrite())
			{
				out.println(Tools.escapeHtml(url+f.getName())+"<br/>");
			}

			checkWritableFolders(url+f.getName()+"/", out);
		}
	}
}


%>
<%@ include file="/admin/layout_top.jsp" %>
<h1>Test zapisovatelnych adresarov, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>

<%
if ("fix".equals(request.getParameter("act"))) {
	checkWritableFolders("/", out);
}
%>

<%@ include file="/admin/layout_bottom.jsp" %>
