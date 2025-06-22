<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>

<h1>Najde nepouzivane pluginy z Metronic temy, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">Spustiť</a></p>

<%
if ("fix".equals(request.getParameter("act"))) {
	//nacitaj si obsah JSP suborov
	StringBuilder code = new StringBuilder();

	IwcmFile webjet8[] = new IwcmFile(Tools.getRealPath("/admin/skins/webjet8/")).listFiles();
	for (IwcmFile f : webjet8)
	{
		if (f.isFile())
		{
			code.append(FileTools.readFileContent(f.getVirtualPath()));
		}
	}

	code.append(FileTools.readFileContent("/admin/skins/webjet8/editor.jsp"));
	code.append(FileTools.readFileContent("/admin/skins/webjet8/ckeditor/config.js"));

	//prejdi si plugin adresar
	for (IwcmFile f : new IwcmFile(Tools.getRealPath("/admin/skins/webjet8/assets/global/plugins/")).listFiles())
	{
		if (f.isDirectory())
		{
			if (code.indexOf("plugins/"+f.getName())==-1)
			{
				out.println(Tools.escapeHtml(f.getVirtualPath())+"<br/>");
			}
		}
	}

	//prejdi si pages adresar
	for (IwcmFile f : new IwcmFile(Tools.getRealPath("/admin/skins/webjet8/assets/admin/pages/scripts/")).listFiles())
	{
		if (f.isFile())
		{
			if (code.indexOf("scripts/"+f.getName())==-1)
			{
				out.println(Tools.escapeHtml(f.getVirtualPath())+"<br/>");
			}
		}
	}

	//media adresar
	for (IwcmFile f : new IwcmFile(Tools.getRealPath("/admin/skins/webjet8/assets/admin/pages/media/")).listFiles())
	{
		if (f.isDirectory())
		{
			if (code.indexOf("media/"+f.getName())==-1)
			{
				out.println(Tools.escapeHtml(f.getVirtualPath())+"<br/>");
			}
		}
	}

	//ckeditor pluginy
	for (IwcmFile f : new IwcmFile(Tools.getRealPath("/admin/skins/webjet8/ckeditor/dist/plugins/")).listFiles())
	{
		if (f.isDirectory())
		{
			if (code.indexOf("//'"+f.getName()+",'")!=-1)
			{
				out.println(Tools.escapeHtml(f.getVirtualPath())+"<br/>");
			}
			else if (code.indexOf(f.getName())==-1)
			{
				out.println(Tools.escapeHtml(f.getVirtualPath())+"<br/>");
			}
		}
	}
}
%>

<%@ include file="/admin/layout_bottom.jsp" %>
