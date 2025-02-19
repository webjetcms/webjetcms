<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.doc.TemplateDetails"%>
<%@page import="sk.iway.iwcm.doc.TemplatesDB"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%
String link = Tools.getRequestParameter(request, "link");
String title = Tools.getRequestParameter(request, "title");
if(Tools.isNotEmpty(link) && Tools.isNotEmpty(link) && (link.startsWith("/files") || link.startsWith("/images")));
{
	IwcmFile subor = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(link));
	if(subor != null && subor.exists())
	{
		Prop prop = null;
		String ext = FileTools.getFileExtension(link).toUpperCase();
		int tempId = Tools.getIntValue(Tools.getRequestParameter(request, "tempId"), -1);
		if (tempId > 0)
		{
			TemplateDetails td = TemplatesDB.getInstance().getTemplate(tempId);
			if (td != null) prop = Prop.getInstance(td.getLng());
		}
		if (prop == null) prop = Prop.getInstance(request);
		String size = FileTools.getFileLength(link);
		if (Tools.isNotEmpty(size))
		{
			if(Tools.isEmpty(title) || title.startsWith(prop.getText("fbrowse.file")))
			{
				out.print(prop.getText("admin.fck_file.subor_velkost", ext, size));
			}
		}
	}
}
%>