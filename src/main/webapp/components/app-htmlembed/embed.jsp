<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.components.apphtmlembed.HtmlEmbedApp,org.apache.commons.codec.binary.Base64"%><%@
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);
	String encoded = pageParams.getValue("html", "");
	String htmlCode = new String(Base64.decodeBase64(encoded), java.nio.charset.StandardCharsets.UTF_8);

	if (htmlCode.startsWith("utf8:"))
	{
		htmlCode = Tools.URLDecode(htmlCode.substring(5));
	}

	//System.out.println("encoded: " + encoded);
	//System.out.println("decoded: " + htmlCode);

	//show HTML/JS Code in editor preview instead of blank page
	boolean showScriptSource = request.getAttribute("inPreviewMode") != null && HtmlEmbedApp.isScriptOnly(htmlCode);
	if (showScriptSource)
	{
		String previewHtmlCode = HtmlEmbedApp.getPreviewHtmlCode(htmlCode, showScriptSource);
		out.print(previewHtmlCode);
	}
	else
	{
%><iwcm:write><%=htmlCode %></iwcm:write><%
	}
%>
