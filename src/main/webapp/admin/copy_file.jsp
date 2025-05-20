
<%@page import="sk.iway.iwcm.filebrowser.BrowseAction"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="menuFbrowser"/>
<%@ include file="/admin/layout_top.jsp" %>
<%
if (Tools.isNotEmpty(Tools.getRequestParameter(request, "from")) && Tools.isNotEmpty(Tools.getRequestParameter(request, "to")))
{
	String fromUrl = Tools.getRequestParameter(request, "from");
	String toUrl = Tools.getRequestParameter(request, "to");

	//ochrana aby nebolo mozne kopirovat hocico hocikde
	if (BrowseAction.hasForbiddenSymbol(fromUrl)==false && BrowseAction.hasForbiddenSymbol(toUrl) && (fromUrl.startsWith("/images") || fromUrl.startsWith("/files")) && (toUrl.startsWith("/images") || toUrl.startsWith("/files")))
	{
		IwcmFile from = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(fromUrl));
		IwcmFile to = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(toUrl));
		FileTools.copyFile(from,to);
	}
}
%>
<%@ include file="/admin/layout_bottom.jsp" %>
