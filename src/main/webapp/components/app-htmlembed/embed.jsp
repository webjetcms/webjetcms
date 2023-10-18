<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,org.apache.commons.codec.binary.Base64"%><%@
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@
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
	Prop prop = Prop.getInstance(lng);
	final String EMPTY_PAGE = prop.getText("components.cloud.app-htmlembed.empty_page");
	String encoded = pageParams.getValue("html", EMPTY_PAGE);
	Base64 b64 = new Base64();
	String htmlCode = new String(b64.decode(encoded.getBytes()));

	if (htmlCode.startsWith("utf8:"))
	{
		htmlCode = Tools.URLDecode(htmlCode.substring(5));
	}

	//System.out.println("encoded: " + encoded);
	//System.out.println("decoded: " + htmlCode);



%><iwcm:write><%=htmlCode %></iwcm:write>
